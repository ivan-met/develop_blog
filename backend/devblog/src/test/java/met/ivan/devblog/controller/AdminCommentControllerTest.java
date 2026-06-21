package met.ivan.devblog.controller;

import tools.jackson.databind.ObjectMapper;
import met.ivan.devblog.config.AppProperties;
import met.ivan.devblog.config.SecurityConfig;
import met.ivan.devblog.dto.AdminCommentResponse;
import met.ivan.devblog.dto.AuthorSummary;
import met.ivan.devblog.security.CustomUserDetailsService;
import met.ivan.devblog.security.JwtAuthenticationFilter;
import met.ivan.devblog.security.JwtService;
import met.ivan.devblog.service.AdminCommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminCommentController.class, GlobalExceptionHandler.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ActiveProfiles("test")
@DisplayName("AdminCommentController")
class AdminCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminCommentService adminCommentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private AdminCommentResponse sampleComment() {
        AuthorSummary author = new AuthorSummary(1L, "testuser", "Test User");
        return new AdminCommentResponse(10L, "Test comment content", author,
                "test-post-slug", "Test Post Title", Instant.now());
    }

    // --- Authorization matrix ---

    @Test
    @DisplayName("GET /api/admin/comments - 401 for anonymous")
    void listComments_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/comments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/admin/comments - 403 for USER role")
    @WithMockUser(roles = "USER")
    void listComments_userRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/comments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/comments - 200 for ADMIN role")
    @WithMockUser(roles = "ADMIN")
    void listComments_adminRole_returns200() throws Exception {
        Page<AdminCommentResponse> page = new PageImpl<>(List.of(sampleComment()), PageRequest.of(0, 20), 1);
        when(adminCommentService.list(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].content").value("Test comment content"))
                .andExpect(jsonPath("$.content[0].postSlug").value("test-post-slug"))
                .andExpect(jsonPath("$.content[0].postTitle").value("Test Post Title"))
                .andExpect(jsonPath("$.content[0].author.username").value("testuser"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/comments?search=java - passes search param to service")
    @WithMockUser(roles = "ADMIN")
    void listComments_withSearch_adminRole_passes_searchParam() throws Exception {
        Page<AdminCommentResponse> page = new PageImpl<>(List.of(sampleComment()), PageRequest.of(0, 20), 1);
        when(adminCommentService.list(eq("java"), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/comments?search=java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    @DisplayName("GET /api/admin/comments - pagination defaults applied")
    @WithMockUser(roles = "ADMIN")
    void listComments_defaultPagination() throws Exception {
        Page<AdminCommentResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(adminCommentService.list(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("GET /api/admin/comments - JSON shape has expected fields")
    @WithMockUser(roles = "ADMIN")
    void listComments_jsonShape() throws Exception {
        Page<AdminCommentResponse> page = new PageImpl<>(List.of(sampleComment()), PageRequest.of(0, 20), 1);
        when(adminCommentService.list(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].content").exists())
                .andExpect(jsonPath("$.content[0].author").exists())
                .andExpect(jsonPath("$.content[0].postSlug").exists())
                .andExpect(jsonPath("$.content[0].postTitle").exists())
                .andExpect(jsonPath("$.content[0].createdAt").exists());
    }
}
