package met.ivan.devblog.controller;

import tools.jackson.databind.ObjectMapper;
import met.ivan.devblog.config.AppProperties;
import met.ivan.devblog.config.SecurityConfig;
import met.ivan.devblog.dto.AuthorSummary;
import met.ivan.devblog.dto.BookmarkResponse;
import met.ivan.devblog.dto.CategoryResponse;
import met.ivan.devblog.dto.LikeResponse;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.exception.ResourceNotFoundException;
import met.ivan.devblog.security.CustomUserDetailsService;
import met.ivan.devblog.security.JwtAuthenticationFilter;
import met.ivan.devblog.security.JwtService;
import met.ivan.devblog.service.EngagementService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {EngagementController.class, GlobalExceptionHandler.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ActiveProfiles("test")
@DisplayName("EngagementController")
class EngagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EngagementService engagementService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private PostSummaryResponse sampleSummary() {
        AuthorSummary author = new AuthorSummary(1L, "testuser", "Test User");
        CategoryResponse cat = new CategoryResponse(1L, "Java", "java", null);
        return new PostSummaryResponse(10L, "test-post", "Test Post", "Excerpt",
                PostStatus.PUBLISHED, cat, author, Instant.now(), Instant.now(),
                Set.of("java"), 10L, 3L);
    }

    // --- POST /api/posts/{slug}/like ---

    @Test
    @DisplayName("POST /api/posts/{slug}/like - anonymous returns 401")
    void like_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/posts/test-post/like"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/posts/{slug}/like - authenticated returns 200 with likeCount and liked")
    @WithMockUser(username = "testuser", roles = "USER")
    void like_authenticated_returns200() throws Exception {
        when(engagementService.like(eq("test-post"), eq("testuser")))
                .thenReturn(new LikeResponse(5, true));

        mockMvc.perform(post("/api/posts/test-post/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(5))
                .andExpect(jsonPath("$.liked").value(true));
    }

    @Test
    @DisplayName("POST /api/posts/{slug}/like - unknown post returns 404")
    @WithMockUser(username = "testuser", roles = "USER")
    void like_unknownPost_returns404() throws Exception {
        when(engagementService.like(eq("missing"), eq("testuser")))
                .thenThrow(new ResourceNotFoundException("Post not found"));

        mockMvc.perform(post("/api/posts/missing/like"))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/posts/{slug}/like ---

    @Test
    @DisplayName("DELETE /api/posts/{slug}/like - anonymous returns 401")
    void unlike_anonymous_returns401() throws Exception {
        mockMvc.perform(delete("/api/posts/test-post/like"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/posts/{slug}/like - authenticated returns 200 with updated count")
    @WithMockUser(username = "testuser", roles = "USER")
    void unlike_authenticated_returns200() throws Exception {
        when(engagementService.unlike(eq("test-post"), eq("testuser")))
                .thenReturn(new LikeResponse(4, false));

        mockMvc.perform(delete("/api/posts/test-post/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.likeCount").value(4));
    }

    // --- POST /api/posts/{slug}/bookmark ---

    @Test
    @DisplayName("POST /api/posts/{slug}/bookmark - anonymous returns 401")
    void bookmark_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/posts/test-post/bookmark"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/posts/{slug}/bookmark - authenticated returns 200 with bookmarked=true")
    @WithMockUser(username = "testuser", roles = "USER")
    void bookmark_authenticated_returns200() throws Exception {
        when(engagementService.bookmark(eq("test-post"), eq("testuser")))
                .thenReturn(new BookmarkResponse(true));

        mockMvc.perform(post("/api/posts/test-post/bookmark"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarked").value(true));
    }

    // --- DELETE /api/posts/{slug}/bookmark ---

    @Test
    @DisplayName("DELETE /api/posts/{slug}/bookmark - authenticated returns 200 with bookmarked=false")
    @WithMockUser(username = "testuser", roles = "USER")
    void removeBookmark_authenticated_returns200() throws Exception {
        when(engagementService.removeBookmark(eq("test-post"), eq("testuser")))
                .thenReturn(new BookmarkResponse(false));

        mockMvc.perform(delete("/api/posts/test-post/bookmark"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarked").value(false));
    }

    // --- GET /api/users/me/bookmarks ---

    @Test
    @DisplayName("GET /api/users/me/bookmarks - anonymous returns 401")
    void listBookmarks_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me/bookmarks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/users/me/bookmarks - authenticated returns 200 with page")
    @WithMockUser(username = "testuser", roles = "USER")
    void listBookmarks_authenticated_returns200() throws Exception {
        Page<PostSummaryResponse> page = new PageImpl<>(List.of(sampleSummary()), PageRequest.of(0, 20), 1);
        when(engagementService.listBookmarks(eq("testuser"), any())).thenReturn(page);

        mockMvc.perform(get("/api/users/me/bookmarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("test-post"));
    }
}
