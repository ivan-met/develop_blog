package met.ivan.devblog.integration;

import tools.jackson.databind.ObjectMapper;
import met.ivan.devblog.dto.*;
import met.ivan.devblog.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("User Management Integration Tests")
class UserManagementIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    // --- Seeding tests ---

    @Test
    @DisplayName("Seeded admin user exists with ADMIN and USER roles")
    void seededAdminExists() {
        assertThat(userRepository.findByUsernameWithRoles("admin")).isPresent().hasValueSatisfying(admin -> {
            assertThat(admin.isActive()).isTrue();
            assertThat(admin.getRoles())
                    .extracting(r -> r.getName().name())
                    .containsExactlyInAnyOrder("ADMIN", "USER");
        });
    }

    @Test
    @DisplayName("Seeded default user exists with USER role only")
    void seededUserExists() {
        assertThat(userRepository.findByUsernameWithRoles("user")).isPresent().hasValueSatisfying(u -> {
            assertThat(u.isActive()).isTrue();
            assertThat(u.getRoles())
                    .extracting(r -> r.getName().name())
                    .containsExactly("USER");
        });
    }

    // --- Registration flow ---

    @Test
    @DisplayName("Register new user returns 201 with tokens")
    void register_newUser_returns201() {
        RegisterRequest request = new RegisterRequest(
                "newuser_it", "newuser_it@test.com", "Password123!");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                baseUrl() + "/api/auth/register",
                new HttpEntity<>(request, jsonHeaders()),
                AuthResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isNotBlank();
        assertThat(response.getBody().getRefreshToken()).isNotBlank();
        assertThat(response.getBody().getUser().getUsername()).isEqualTo("newuser_it");
        assertThat(response.getBody().getUser().getRoles()).contains("USER");
    }

    @Test
    @DisplayName("Register with duplicate username returns 409")
    void register_duplicateUsername_returns409() {
        // First register
        RegisterRequest first = new RegisterRequest(
                "dupuser", "dup1@test.com", "Password123!");
        restTemplate.postForEntity(baseUrl() + "/api/auth/register",
                new HttpEntity<>(first, jsonHeaders()), AuthResponse.class);

        // Second register with same username
        RegisterRequest second = new RegisterRequest(
                "dupuser", "dup2@test.com", "Password123!");
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl() + "/api/auth/register",
                new HttpEntity<>(second, jsonHeaders()),
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // --- Full auth flow ---

    @Test
    @DisplayName("Full flow: register -> login -> get profile -> update profile -> change password -> refresh -> logout")
    void fullAuthFlow() {
        // 1. Register
        RegisterRequest regReq = new RegisterRequest(
                "flowuser", "flowuser@test.com", "FlowPass123!");
        ResponseEntity<AuthResponse> regResp = restTemplate.postForEntity(
                baseUrl() + "/api/auth/register",
                new HttpEntity<>(regReq, jsonHeaders()),
                AuthResponse.class
        );
        assertThat(regResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String accessToken = regResp.getBody().getAccessToken();
        String refreshToken = regResp.getBody().getRefreshToken();

        // 2. GET /api/users/me
        ResponseEntity<UserResponse> profileResp = restTemplate.exchange(
                baseUrl() + "/api/users/me",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(accessToken)),
                UserResponse.class
        );
        assertThat(profileResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profileResp.getBody().getUsername()).isEqualTo("flowuser");

        // 3. PUT /api/users/me
        UpdateProfileRequest updateReq = new UpdateProfileRequest(
                "Flow User", "I love coding", null, null);
        ResponseEntity<UserResponse> updateResp = restTemplate.exchange(
                baseUrl() + "/api/users/me",
                HttpMethod.PUT,
                new HttpEntity<>(updateReq, bearerHeaders(accessToken)),
                UserResponse.class
        );
        assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResp.getBody().getDisplayName()).isEqualTo("Flow User");

        // 4. Change password
        ChangePasswordRequest pwReq = new ChangePasswordRequest("FlowPass123!", "NewFlowPass456!");
        ResponseEntity<Void> pwResp = restTemplate.exchange(
                baseUrl() + "/api/users/me/password",
                HttpMethod.PUT,
                new HttpEntity<>(pwReq, bearerHeaders(accessToken)),
                Void.class
        );
        assertThat(pwResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 5. Refresh token
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(refreshToken);
        ResponseEntity<TokenRefreshResponse> refreshResp = restTemplate.postForEntity(
                baseUrl() + "/api/auth/refresh",
                new HttpEntity<>(refreshReq, jsonHeaders()),
                TokenRefreshResponse.class
        );
        assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String newAccessToken = refreshResp.getBody().getAccessToken();
        String newRefreshToken = refreshResp.getBody().getRefreshToken();
        assertThat(newAccessToken).isNotBlank();
        assertThat(newRefreshToken).isNotEqualTo(refreshToken); // token rotated

        // 6. Old refresh token is rejected
        ResponseEntity<ErrorResponse> oldRefreshResp = restTemplate.postForEntity(
                baseUrl() + "/api/auth/refresh",
                new HttpEntity<>(new RefreshTokenRequest(refreshToken), jsonHeaders()),
                ErrorResponse.class
        );
        assertThat(oldRefreshResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 7. Logout
        ResponseEntity<Void> logoutResp = restTemplate.postForEntity(
                baseUrl() + "/api/auth/logout",
                new HttpEntity<>(new RefreshTokenRequest(newRefreshToken), jsonHeaders()),
                Void.class
        );
        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // --- RBAC tests ---

    @Test
    @DisplayName("USER token cannot access /api/admin/users (403)")
    void regularUser_cannotAccessAdminEndpoint() {
        // Register + login as regular user
        RegisterRequest regReq = new RegisterRequest(
                "regularuser_rbac", "regular_rbac@test.com", "Password123!");
        AuthResponse regResp = restTemplate.postForEntity(
                baseUrl() + "/api/auth/register",
                new HttpEntity<>(regReq, jsonHeaders()),
                AuthResponse.class
        ).getBody();

        ResponseEntity<ErrorResponse> adminResp = restTemplate.exchange(
                baseUrl() + "/api/admin/users",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(regResp.getAccessToken())),
                ErrorResponse.class
        );
        assertThat(adminResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Seeded admin can access /api/admin/users (200)")
    void seededAdmin_canAccessAdminEndpoint() {
        // Login as seeded admin
        LoginRequest loginReq = new LoginRequest("admin", "Admin@1234");
        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login",
                new HttpEntity<>(loginReq, jsonHeaders()),
                AuthResponse.class
        );
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String adminToken = loginResp.getBody().getAccessToken();

        ResponseEntity<String> adminUsersResp = restTemplate.exchange(
                baseUrl() + "/api/admin/users",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminToken)),
                String.class
        );
        assertThat(adminUsersResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 JSON error")
    void unauthenticated_returns401Json() {
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                baseUrl() + "/api/users/me",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                ErrorResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Login with wrong password returns 401")
    void login_wrongPassword_returns401() {
        LoginRequest request = new LoginRequest("admin", "wrongpassword");
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login",
                new HttpEntity<>(request, jsonHeaders()),
                ErrorResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
