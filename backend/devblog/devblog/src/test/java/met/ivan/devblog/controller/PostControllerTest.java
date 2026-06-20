package met.ivan.devblog.controller;

import tools.jackson.databind.ObjectMapper;
import met.ivan.devblog.config.AppProperties;
import met.ivan.devblog.config.SecurityConfig;
import met.ivan.devblog.dto.AuthorSummary;
import met.ivan.devblog.dto.CategoryResponse;
import met.ivan.devblog.dto.CreatePostRequest;
import met.ivan.devblog.dto.PostResponse;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.exception.ForbiddenOperationException;
import met.ivan.devblog.exception.ResourceNotFoundException;
import met.ivan.devblog.security.CustomUserDetailsService;
import met.ivan.devblog.security.JwtAuthenticationFilter;
import met.ivan.devblog.security.JwtService;
import met.ivan.devblog.service.PostService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PostController.class, GlobalExceptionHandler.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ActiveProfiles("test")
@DisplayName("PostController")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private PostSummaryResponse sampleSummary() {
        AuthorSummary author = new AuthorSummary(1L, "testuser", "Test User");
        CategoryResponse cat = new CategoryResponse(1L, "Java", "java", null);
        return new PostSummaryResponse(10L, "test-post", "Test Post", "Excerpt",
                PostStatus.PUBLISHED, cat, author, Instant.now(), Instant.now());
    }

    private PostResponse sampleResponse() {
        AuthorSummary author = new AuthorSummary(1L, "testuser", "Test User");
        CategoryResponse cat = new CategoryResponse(1L, "Java", "java", null);
        return new PostResponse(10L, "test-post", "Test Post", "Excerpt",
                PostStatus.PUBLISHED, cat, author, Instant.now(), Instant.now(),
                "# Content", Instant.now());
    }

    // --- Public GET /api/posts ---

    @Test
    @DisplayName("GET /api/posts - anonymous can list published posts (200)")
    void listPublished_anonymous_returns200() throws Exception {
        Page<PostSummaryResponse> page = new PageImpl<>(List.of(sampleSummary()), PageRequest.of(0, 20), 1);
        when(postService.listPublished(isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("test-post"));
    }

    @Test
    @DisplayName("GET /api/posts/{slug} - anonymous can get published post (200)")
    void getBySlug_anonymous_returns200() throws Exception {
        when(postService.getPublishedBySlug("test-post")).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/posts/test-post"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("test-post"))
                .andExpect(jsonPath("$.contentMarkdown").value("# Content"));
    }

    @Test
    @DisplayName("GET /api/posts/{slug} - draft returns 404 for anonymous")
    void getBySlug_draft_returns404() throws Exception {
        when(postService.getPublishedBySlug("draft-post"))
                .thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/api/posts/draft-post"))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/posts ---

    @Test
    @DisplayName("POST /api/posts - anonymous returns 401")
    void createPost_anonymous_returns401() throws Exception {
        CreatePostRequest req = new CreatePostRequest("Title", "Content", null, null, PostStatus.DRAFT);

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/posts - USER role returns 201")
    @WithMockUser(username = "testuser", roles = "USER")
    void createPost_user_returns201() throws Exception {
        CreatePostRequest req = new CreatePostRequest("Title", "Content", null, null, PostStatus.DRAFT);
        when(postService.create(eq("testuser"), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("test-post"));
    }

    @Test
    @DisplayName("POST /api/posts - ADMIN role returns 403 (service throws ForbiddenOperationException)")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createPost_admin_returns403() throws Exception {
        CreatePostRequest req = new CreatePostRequest("Title", "Content", null, null, PostStatus.DRAFT);
        when(postService.create(eq("admin"), any()))
                .thenThrow(new ForbiddenOperationException("Administrators are not permitted to author posts"));

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Administrators are not permitted to author posts"));
    }

    // --- GET /api/posts/mine ---

    @Test
    @DisplayName("GET /api/posts/mine - anonymous returns 401")
    void listMine_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/posts/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/posts/mine - USER returns 200")
    @WithMockUser(username = "testuser", roles = "USER")
    void listMine_user_returns200() throws Exception {
        Page<PostSummaryResponse> page = new PageImpl<>(List.of(sampleSummary()), PageRequest.of(0, 20), 1);
        when(postService.listOwn(any(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/posts/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("test-post"));
    }

    // --- DELETE /api/posts/{id} ---

    @Test
    @DisplayName("DELETE /api/posts/{id} - anonymous returns 401")
    void deletePost_anonymous_returns401() throws Exception {
        mockMvc.perform(delete("/api/posts/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/posts/{id} - owner returns 204")
    @WithMockUser(username = "testuser", roles = "USER")
    void deletePost_owner_returns204() throws Exception {
        mockMvc.perform(delete("/api/posts/10"))
                .andExpect(status().isNoContent());
    }
}
