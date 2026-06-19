package met.ivan.devblog.controller;

import tools.jackson.databind.ObjectMapper;
import met.ivan.devblog.config.AppProperties;
import met.ivan.devblog.config.SecurityConfig;
import met.ivan.devblog.dto.UpdateRolesRequest;
import met.ivan.devblog.dto.UpdateStatusRequest;
import met.ivan.devblog.dto.UserResponse;
import met.ivan.devblog.security.CustomUserDetailsService;
import met.ivan.devblog.security.JwtAuthenticationFilter;
import met.ivan.devblog.security.JwtService;
import met.ivan.devblog.service.AdminUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AdminUserController.class, GlobalExceptionHandler.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ActiveProfiles("test")
@DisplayName("AdminUserController")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UserResponse sampleUser() {
        return new UserResponse(1L, "user1", "user1@test.com",
                null, null, null, List.of("USER"), true, Instant.now());
    }

    // --- Authorization matrix ---

    @Test
    @DisplayName("GET /api/admin/users - 401 for anonymous")
    void listUsers_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/admin/users - 403 for USER role")
    @WithMockUser(roles = "USER")
    void listUsers_userRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/users - 200 for ADMIN role")
    @WithMockUser(roles = "ADMIN")
    void listUsers_adminRole_returns200() throws Exception {
        Page<UserResponse> page = new PageImpl<>(List.of(sampleUser()), PageRequest.of(0, 20), 1);
        when(adminUserService.listUsers(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("user1"));
    }

    @Test
    @DisplayName("GET /api/admin/users/{id} - 403 for USER role")
    @WithMockUser(roles = "USER")
    void getUser_userRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/users/{id} - 200 for ADMIN role")
    @WithMockUser(roles = "ADMIN")
    void getUser_adminRole_returns200() throws Exception {
        when(adminUserService.getUser(1L)).thenReturn(sampleUser());

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/roles - 403 for USER role")
    @WithMockUser(roles = "USER")
    void updateRoles_userRole_returns403() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRolesRequest(List.of("ADMIN")))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/roles - 200 for ADMIN role")
    @WithMockUser(roles = "ADMIN")
    void updateRoles_adminRole_returns200() throws Exception {
        when(adminUserService.updateRoles(eq(1L), any())).thenReturn(sampleUser());

        mockMvc.perform(put("/api/admin/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRolesRequest(List.of("USER")))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/admin/users/{id}/status - 200 for ADMIN role")
    @WithMockUser(roles = "ADMIN")
    void updateStatus_adminRole_returns200() throws Exception {
        when(adminUserService.updateStatus(eq(1L), any())).thenReturn(sampleUser());

        mockMvc.perform(put("/api/admin/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest(false))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/admin/users with search param - ADMIN role")
    @WithMockUser(roles = "ADMIN")
    void listUsers_withSearch_adminRole_returns200() throws Exception {
        Page<UserResponse> page = new PageImpl<>(List.of(sampleUser()), PageRequest.of(0, 20), 1);
        when(adminUserService.listUsers(eq("alice"), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/users?search=alice"))
                .andExpect(status().isOk());
    }
}
