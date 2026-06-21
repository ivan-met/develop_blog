package met.ivan.devblog.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET = "test-secret-key-for-testing-only-must-be-256-bits-long-padding!!!";
    private static final long ACCESS_EXPIRY = 900_000L;   // 15m
    private static final long REFRESH_EXPIRY = 604_800_000L; // 7d

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ACCESS_EXPIRY, REFRESH_EXPIRY);
        userDetails = User.builder()
                .username("alice")
                .password("pass")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();
    }

    @Test
    @DisplayName("generates access token and extracts username")
    void generateAccessToken_roundTrip() {
        String token = jwtService.generateAccessToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    @DisplayName("access token includes roles claim")
    void generateAccessToken_includesRoles() {
        String token = jwtService.generateAccessToken(userDetails);

        List<String> roles = jwtService.extractRoles(token);
        assertThat(roles).contains("ROLE_USER");
    }

    @Test
    @DisplayName("generates refresh token and extracts username")
    void generateRefreshToken_roundTrip() {
        String token = jwtService.generateRefreshToken(userDetails);

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    @DisplayName("isTokenValid returns true for valid token")
    void isTokenValid_validToken() {
        String token = jwtService.generateAccessToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid returns false for wrong user")
    void isTokenValid_wrongUser() {
        String token = jwtService.generateAccessToken(userDetails);

        UserDetails other = User.builder()
                .username("bob")
                .password("pass")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();

        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    @DisplayName("expired token is rejected")
    void expiredToken_isRejected() {
        JwtService shortLived = new JwtService(SECRET, 1L, REFRESH_EXPIRY); // 1ms expiry
        String token = shortLived.generateAccessToken(userDetails);

        // Sleep briefly to ensure expiry
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThat(shortLived.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    @DisplayName("tampered token is rejected")
    void tamperedToken_isRejected() {
        String token = jwtService.generateAccessToken(userDetails);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtService.isTokenValid(token.equals(tampered) ? "invalid" : tampered)).isFalse();
    }

    @Test
    @DisplayName("completely invalid token string is rejected")
    void invalidTokenString_isRejected() {
        assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
    }
}
