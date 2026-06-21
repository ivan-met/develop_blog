package met.ivan.devblog.controller;

import met.ivan.devblog.config.AppProperties;
import met.ivan.devblog.config.SecurityConfig;
import met.ivan.devblog.dto.AuthorSummary;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.security.CustomUserDetailsService;
import met.ivan.devblog.security.JwtAuthenticationFilter;
import met.ivan.devblog.security.JwtService;
import met.ivan.devblog.service.AdminPostService;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminPostController.class, GlobalExceptionHandler.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ActiveProfiles("test")
@DisplayName("AdminPostController")
class AdminPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminPostService adminPostService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private PostSummaryResponse samplePost(PostStatus status) {
        AuthorSummary author = new AuthorSummary(1L, "testuser", "Test User");
        return new PostSummaryResponse(10L, "test-post", "Test Post Title", "Excerpt",
                status, null, author, Instant.now(), Instant.now(), Set.of("java"), 42L, 5L);
    }

    // --- Authorization matrix ---

    @Test
    @DisplayName("GET /api/admin/posts - 401 for anonymous")
    void listPosts_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/posts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/admin/posts - 403 for USER role")
    @WithMockUser(roles = "USER")
    void listPosts_userRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/posts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/posts - 200 for ADMIN role")
    @WithMockUser(roles = "ADMIN")
    void listPosts_adminRole_returns200() throws Exception {
        Page<PostSummaryResponse> page = new PageImpl<>(
                List.of(samplePost(PostStatus.PUBLISHED), samplePost(PostStatus.DRAFT)),
                PageRequest.of(0, 20), 2);
        when(adminPostService.list(isNull(), isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].slug").value("test-post"))
                .andExpect(jsonPath("$.content[0].author.username").value("testuser"))
                .andExpect(jsonPath("$.content[0].viewCount").value(42))
                .andExpect(jsonPath("$.content[0].likeCount").value(5));
    }

    @Test
    @DisplayName("GET /api/admin/posts?status=DRAFT - passes status param to service")
    @WithMockUser(roles = "ADMIN")
    void listPosts_withStatusFilter_adminRole() throws Exception {
        Page<PostSummaryResponse> page = new PageImpl<>(
                List.of(samplePost(PostStatus.DRAFT)), PageRequest.of(0, 20), 1);
        when(adminPostService.list(eq(PostStatus.DRAFT), isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/posts?status=DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
    }

    @Test
    @DisplayName("GET /api/admin/posts?search=spring - passes search param to service")
    @WithMockUser(roles = "ADMIN")
    void listPosts_withSearch_adminRole() throws Exception {
        Page<PostSummaryResponse> page = new PageImpl<>(
                List.of(samplePost(PostStatus.PUBLISHED)), PageRequest.of(0, 20), 1);
        when(adminPostService.list(isNull(), eq("spring"), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/posts?search=spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/posts?categorySlug=java - passes categorySlug to service")
    @WithMockUser(roles = "ADMIN")
    void listPosts_withCategorySlug_adminRole() throws Exception {
        Page<PostSummaryResponse> page = new PageImpl<>(
                List.of(samplePost(PostStatus.PUBLISHED)), PageRequest.of(0, 20), 1);
        when(adminPostService.list(isNull(), isNull(), eq("java"), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/posts?categorySlug=java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/posts - JSON shape has expected fields")
    @WithMockUser(roles = "ADMIN")
    void listPosts_jsonShape() throws Exception {
        Page<PostSummaryResponse> page = new PageImpl<>(
                List.of(samplePost(PostStatus.PUBLISHED)), PageRequest.of(0, 20), 1);
        when(adminPostService.list(isNull(), isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].slug").exists())
                .andExpect(jsonPath("$.content[0].title").exists())
                .andExpect(jsonPath("$.content[0].status").exists())
                .andExpect(jsonPath("$.content[0].author").exists())
                .andExpect(jsonPath("$.content[0].viewCount").exists())
                .andExpect(jsonPath("$.content[0].likeCount").exists());
    }
}
