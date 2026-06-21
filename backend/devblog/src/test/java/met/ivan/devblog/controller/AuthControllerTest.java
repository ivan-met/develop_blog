package met.ivan.devblog.controller;

import tools.jackson.databind.ObjectMapper;
import met.ivan.devblog.config.AppProperties;
import met.ivan.devblog.config.SecurityConfig;
import met.ivan.devblog.dto.AuthResponse;
import met.ivan.devblog.dto.LoginRequest;
import met.ivan.devblog.dto.RegisterRequest;
import met.ivan.devblog.dto.UserResponse;
import met.ivan.devblog.security.CustomUserDetailsService;
import met.ivan.devblog.security.JwtAuthenticationFilter;
import met.ivan.devblog.security.JwtService;
import met.ivan.devblog.service.AuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, GlobalExceptionHandler.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ActiveProfiles("test")
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UserResponse sampleUser() {
        return new UserResponse(1L, "alice", "alice@test.com",
                null, null, null, List.of("USER"), true, Instant.now());
    }

    @Test
    @DisplayName("POST /api/auth/register - 201 with valid request")
    void register_validRequest_returns201() throws Exception {
        AuthResponse response = new AuthResponse("access-token", "refresh-token", sampleUser());
        when(authenticationService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("alice", "alice@test.com", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.user.username").value("alice"));
    }

    @Test
    @DisplayName("POST /api/auth/register - 400 with blank username")
    void register_blankUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","email":"alice@test.com","password":"password123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("POST /api/auth/register - 400 with invalid email")
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"not-an-email","password":"password123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register - 400 with short password")
    void register_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"alice@test.com","password":"short"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login - 200 with valid credentials")
    void login_validCredentials_returns200() throws Exception {
        AuthResponse response = new AuthResponse("access-token", "refresh-token", sampleUser());
        when(authenticationService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("alice", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("POST /api/auth/login - 400 with blank username")
    void login_blankUsernameOrEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"","password":"pass123"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
