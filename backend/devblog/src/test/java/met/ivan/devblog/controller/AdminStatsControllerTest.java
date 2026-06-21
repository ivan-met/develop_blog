package met.ivan.devblog.controller;

import met.ivan.devblog.config.AppProperties;
import met.ivan.devblog.config.SecurityConfig;
import met.ivan.devblog.dto.AuthorSummary;
import met.ivan.devblog.dto.PlatformStatsResponse;
import met.ivan.devblog.dto.RecentUserResponse;
import met.ivan.devblog.dto.TopPostResponse;
import met.ivan.devblog.security.CustomUserDetailsService;
import met.ivan.devblog.security.JwtAuthenticationFilter;
import met.ivan.devblog.security.JwtService;
import met.ivan.devblog.service.AdminStatsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminStatsController.class, GlobalExceptionHandler.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ActiveProfiles("test")
@DisplayName("AdminStatsController")
class AdminStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminStatsService adminStatsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private PlatformStatsResponse sampleStats() {
        PlatformStatsResponse.StatsTotals totals = new PlatformStatsResponse.StatsTotals(
                10L, 8L, 20L, 15L, 5L, 50L, 4L, 100L, 30L);

        AuthorSummary authorSummary = new AuthorSummary(1L, "testuser", "Test User");
        TopPostResponse topPost = new TopPostResponse("top-post", "Top Post Title", authorSummary, 42L);
        RecentUserResponse recentUser = new RecentUserResponse("newuser", "New User", Instant.now());

        return new PlatformStatsResponse(totals, List.of(), List.of(topPost), List.of(recentUser));
    }

    // --- Authorization matrix ---

    @Test
    @DisplayName("GET /api/admin/stats - 401 for anonymous")
    void getStats_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/admin/stats - 403 for USER role")
    @WithMockUser(roles = "USER")
    void getStats_userRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/stats - 200 for ADMIN role")
    @WithMockUser(roles = "ADMIN")
    void getStats_adminRole_returns200() throws Exception {
        when(adminStatsService.getStats()).thenReturn(sampleStats());

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.users").value(10))
                .andExpect(jsonPath("$.totals.activeUsers").value(8))
                .andExpect(jsonPath("$.totals.posts").value(20))
                .andExpect(jsonPath("$.totals.publishedPosts").value(15))
                .andExpect(jsonPath("$.totals.draftPosts").value(5))
                .andExpect(jsonPath("$.totals.comments").value(50))
                .andExpect(jsonPath("$.totals.categories").value(4))
                .andExpect(jsonPath("$.totals.likes").value(100))
                .andExpect(jsonPath("$.totals.bookmarks").value(30));
    }

    @Test
    @DisplayName("GET /api/admin/stats - topPostsByLikes JSON shape is correct")
    @WithMockUser(roles = "ADMIN")
    void getStats_topByLikes_jsonShape() throws Exception {
        when(adminStatsService.getStats()).thenReturn(sampleStats());

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topPostsByLikes[0].slug").value("top-post"))
                .andExpect(jsonPath("$.topPostsByLikes[0].title").value("Top Post Title"))
                .andExpect(jsonPath("$.topPostsByLikes[0].likeCount").value(42))
                .andExpect(jsonPath("$.topPostsByLikes[0].author.username").value("testuser"));
    }

    @Test
    @DisplayName("GET /api/admin/stats - recentUsers JSON shape is correct")
    @WithMockUser(roles = "ADMIN")
    void getStats_recentUsers_jsonShape() throws Exception {
        when(adminStatsService.getStats()).thenReturn(sampleStats());

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentUsers[0].username").value("newuser"))
                .andExpect(jsonPath("$.recentUsers[0].displayName").value("New User"))
                .andExpect(jsonPath("$.recentUsers[0].createdAt").exists());
    }

    @Test
    @DisplayName("GET /api/admin/stats - full response shape has all top-level fields")
    @WithMockUser(roles = "ADMIN")
    void getStats_fullShape() throws Exception {
        when(adminStatsService.getStats()).thenReturn(sampleStats());

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals").exists())
                .andExpect(jsonPath("$.topPostsByViews").exists())
                .andExpect(jsonPath("$.topPostsByLikes").exists())
                .andExpect(jsonPath("$.recentUsers").exists());
    }
}
