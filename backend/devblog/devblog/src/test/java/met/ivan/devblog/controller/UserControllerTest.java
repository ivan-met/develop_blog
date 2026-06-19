package met.ivan.devblog.controller;

import tools.jackson.databind.ObjectMapper;
import met.ivan.devblog.config.AppProperties;
import met.ivan.devblog.config.SecurityConfig;
import met.ivan.devblog.dto.UpdateProfileRequest;
import met.ivan.devblog.dto.UserResponse;
import met.ivan.devblog.security.CustomUserDetailsService;
import met.ivan.devblog.security.JwtAuthenticationFilter;
import met.ivan.devblog.security.JwtService;
import met.ivan.devblog.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {UserController.class, GlobalExceptionHandler.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ActiveProfiles("test")
@DisplayName("UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UserResponse sampleResponse() {
        return new UserResponse(1L, "testuser", "test@example.com",
                "Test User", "Bio", null, List.of("USER"), true, Instant.now());
    }

    @Test
    @DisplayName("GET /api/users/me - 401 without authentication")
    void getProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/users/me - 200 with authenticated user")
    @WithMockUser(username = "testuser", roles = "USER")
    void getProfile_authenticated_returns200() throws Exception {
        when(userService.getProfile("testuser")).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("PUT /api/users/me - 200 updates profile")
    @WithMockUser(username = "testuser", roles = "USER")
    void updateProfile_valid_returns200() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("New Name", "New bio", null, null);
        when(userService.updateProfile(eq("testuser"), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/users/me/password - 204 on success")
    @WithMockUser(username = "testuser", roles = "USER")
    void changePassword_valid_returns204() throws Exception {
        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"current123","newPassword":"newPass123"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /api/users/me/password - 400 when current password blank")
    @WithMockUser(username = "testuser", roles = "USER")
    void changePassword_blankCurrentPassword_returns400() throws Exception {
        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"","newPassword":"newPass123"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
