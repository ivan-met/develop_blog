package met.ivan.devblog.service;

import met.ivan.devblog.TestDataFactory;
import met.ivan.devblog.dto.ChangePasswordRequest;
import met.ivan.devblog.dto.UpdateProfileRequest;
import met.ivan.devblog.dto.UserResponse;
import met.ivan.devblog.entity.Role;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.exception.BadCredentialsException;
import met.ivan.devblog.exception.DuplicateResourceException;
import met.ivan.devblog.exception.ResourceNotFoundException;
import met.ivan.devblog.mapper.UserMapper;
import met.ivan.devblog.repository.UserRepository;
import met.ivan.devblog.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private Role userRole;
    private User testUser;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        userRole = TestDataFactory.userRole();
        testUser = TestDataFactory.userWithRole(userRole);
        testUserResponse = new UserResponse(1L, "testuser", "test@example.com",
                "Test User", "Test bio", null, List.of("USER"), true, Instant.now());
    }

    @Test
    @DisplayName("getProfile: returns user response for existing user")
    void getProfile_existingUser() {
        when(userRepository.findByUsernameWithRoles("testuser")).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        UserResponse result = userService.getProfile("testuser");

        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("getProfile: throws ResourceNotFoundException for unknown user")
    void getProfile_unknownUser() {
        when(userRepository.findByUsernameWithRoles("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateProfile: updates display name and bio")
    void updateProfile_updatesFields() {
        UpdateProfileRequest request = new UpdateProfileRequest("New Name", "New bio", null, null);
        when(userRepository.findByUsernameWithRoles("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        userService.updateProfile("testuser", request);

        assertThat(testUser.getDisplayName()).isEqualTo("New Name");
        assertThat(testUser.getBio()).isEqualTo("New bio");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateProfile: email change checks for uniqueness")
    void updateProfile_emailChangeUniqueCheck() {
        UpdateProfileRequest request = new UpdateProfileRequest(null, null, null, "taken@test.com");
        when(userRepository.findByUsernameWithRoles("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailAndIdNot("taken@test.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile("testuser", request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    @DisplayName("changePassword: correct current password succeeds")
    void changePassword_correctPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("currentPass", "newPassword123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPass", testUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newHash");

        userService.changePassword("testuser", request);

        assertThat(testUser.getPasswordHash()).isEqualTo("newHash");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("changePassword: wrong current password throws BadCredentialsException")
    void changePassword_wrongPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPass", "newPassword123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPass", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("testuser", request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("incorrect");
    }
}
