package met.ivan.devblog.service;

import met.ivan.devblog.TestDataFactory;
import met.ivan.devblog.dto.*;
import met.ivan.devblog.entity.RefreshToken;
import met.ivan.devblog.entity.Role;
import met.ivan.devblog.entity.RoleName;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.exception.BadCredentialsException;
import met.ivan.devblog.exception.DuplicateResourceException;
import met.ivan.devblog.exception.InvalidTokenException;
import met.ivan.devblog.mapper.UserMapper;
import met.ivan.devblog.repository.RefreshTokenRepository;
import met.ivan.devblog.repository.RoleRepository;
import met.ivan.devblog.repository.UserRepository;
import met.ivan.devblog.security.JwtService;
import met.ivan.devblog.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationServiceImpl")
class AuthenticationServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private AuthenticationServiceImpl authService;

    private Role userRole;
    private Role adminRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 604800000L);
        userRole = TestDataFactory.userRole();
        adminRole = TestDataFactory.adminRole();
        testUser = TestDataFactory.userWithRole(userRole);
    }

    // --- Register ---

    @Test
    @DisplayName("register: happy path creates user with USER role")
    void register_happyPath() {
        RegisterRequest request = new RegisterRequest("alice", "alice@test.com", "password123");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        when(userMapper.toResponse(any())).thenReturn(
                new UserResponse(1L, "alice", "alice@test.com", null, null, null, List.of("USER"), true, Instant.now()));

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser()).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("register: duplicate username throws DuplicateResourceException")
    void register_duplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice", "alice@test.com", "password")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    @DisplayName("register: duplicate email throws DuplicateResourceException")
    void register_duplicateEmail() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice", "alice@test.com", "password")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");
    }

    // --- Login ---

    @Test
    @DisplayName("login: valid credentials returns tokens")
    void login_validCredentials() {
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(userRepository.findByUsernameOrEmailWithRoles("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
        when(userMapper.toResponse(testUser)).thenReturn(
                new UserResponse(1L, "testuser", "test@example.com", null, null, null, List.of("USER"), true, Instant.now()));

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access");
        verify(refreshTokenRepository).save(any());
    }

    @Test
    @DisplayName("login: wrong password throws BadCredentialsException")
    void login_wrongPassword() {
        when(userRepository.findByUsernameOrEmailWithRoles("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("testuser", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login: unknown user throws BadCredentialsException")
    void login_unknownUser() {
        when(userRepository.findByUsernameOrEmailWithRoles("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", "pass")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login: inactive user throws BadCredentialsException")
    void login_inactiveUser() {
        User inactive = User.builder()
                .username("inactive")
                .email("inactive@test.com")
                .passwordHash("hash")
                .active(false)
                .roles(new java.util.HashSet<>())
                .build();
        when(userRepository.findByUsernameOrEmailWithRoles("inactive")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> authService.login(new LoginRequest("inactive", "pass")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("disabled");
    }

    // --- Refresh ---

    @Test
    @DisplayName("refresh: valid token rotates and returns new tokens")
    void refresh_validToken() {
        RefreshToken storedToken = RefreshToken.builder()
                .token("valid-refresh")
                .user(testUser)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("valid-refresh")).thenReturn(Optional.of(storedToken));
        when(jwtService.isTokenValid("valid-refresh")).thenReturn(true);
        when(userRepository.findByIdWithRoles(testUser.getId())).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(any())).thenReturn("new-access");
        when(jwtService.generateRefreshToken(any())).thenReturn("new-refresh");

        TokenRefreshResponse response = authService.refresh(new RefreshTokenRequest("valid-refresh"));

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        assertThat(storedToken.isRevoked()).isTrue(); // old token revoked
    }

    @Test
    @DisplayName("refresh: revoked token throws InvalidTokenException")
    void refresh_revokedToken() {
        RefreshToken revoked = RefreshToken.builder()
                .token("revoked")
                .user(testUser)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByToken("revoked")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("revoked")))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("refresh: expired token throws InvalidTokenException")
    void refresh_expiredToken() {
        RefreshToken expired = RefreshToken.builder()
                .token("expired")
                .user(testUser)
                .expiresAt(Instant.now().minusSeconds(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByToken("expired")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("expired")))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    // --- Logout ---

    @Test
    @DisplayName("logout: revokes the refresh token")
    void logout_revokesToken() {
        RefreshToken token = RefreshToken.builder()
                .token("to-revoke")
                .user(testUser)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByToken("to-revoke")).thenReturn(Optional.of(token));

        authService.logout(new RefreshTokenRequest("to-revoke"));

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    @DisplayName("logout: non-existent token is silently ignored")
    void logout_nonExistentToken() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        authService.logout(new RefreshTokenRequest("unknown"));

        verify(refreshTokenRepository, never()).save(any());
    }
}
