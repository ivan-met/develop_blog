package met.ivan.devblog.controller;

import tools.jackson.databind.ObjectMapper;
import met.ivan.devblog.config.AppProperties;
import met.ivan.devblog.config.SecurityConfig;
import met.ivan.devblog.dto.AuthorSummary;
import met.ivan.devblog.dto.CommentResponse;
import met.ivan.devblog.dto.CreateCommentRequest;
import met.ivan.devblog.exception.ForbiddenOperationException;
import met.ivan.devblog.exception.ResourceNotFoundException;
import met.ivan.devblog.security.CustomUserDetailsService;
import met.ivan.devblog.security.JwtAuthenticationFilter;
import met.ivan.devblog.security.JwtService;
import met.ivan.devblog.service.CommentService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {CommentController.class, GlobalExceptionHandler.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ActiveProfiles("test")
@DisplayName("CommentController")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CommentResponse sampleComment() {
        return new CommentResponse(1L, "Great post!", new AuthorSummary(1L, "testuser", "Test User"),
                Instant.now(), false);
    }

    @Test
    @DisplayName("GET /api/posts/{slug}/comments - anonymous can list (200)")
    void listComments_anonymous_returns200() throws Exception {
        Page<CommentResponse> page = new PageImpl<>(List.of(sampleComment()), PageRequest.of(0, 20), 1);
        when(commentService.list(eq("test-post"), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/posts/test-post/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("Great post!"))
                .andExpect(jsonPath("$.content[0].canDelete").value(false));
    }

    @Test
    @DisplayName("GET /api/posts/{slug}/comments - authenticated sees canDelete=true for own comment")
    @WithMockUser(username = "testuser", roles = "USER")
    void listComments_authenticated_seesCandDelete() throws Exception {
        CommentResponse myComment = new CommentResponse(1L, "My comment",
                new AuthorSummary(1L, "testuser", "Test User"), Instant.now(), true);
        Page<CommentResponse> page = new PageImpl<>(List.of(myComment), PageRequest.of(0, 20), 1);
        when(commentService.list(eq("test-post"), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/posts/test-post/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].canDelete").value(true));
    }

    @Test
    @DisplayName("GET /api/posts/{slug}/comments - not found post returns 404")
    void listComments_notFoundPost_returns404() throws Exception {
        when(commentService.list(eq("missing"), isNull(), any()))
                .thenThrow(new ResourceNotFoundException("Post not found"));

        mockMvc.perform(get("/api/posts/missing/comments"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/posts/{slug}/comments - anonymous returns 401")
    void createComment_anonymous_returns401() throws Exception {
        CreateCommentRequest req = new CreateCommentRequest("Hello");
        mockMvc.perform(post("/api/posts/test-post/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/posts/{slug}/comments - authenticated returns 201")
    @WithMockUser(username = "testuser", roles = "USER")
    void createComment_authenticated_returns201() throws Exception {
        CreateCommentRequest req = new CreateCommentRequest("Hello world!");
        when(commentService.create(eq("test-post"), eq("testuser"), any()))
                .thenReturn(sampleComment());

        mockMvc.perform(post("/api/posts/test-post/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Great post!"));
    }

    @Test
    @DisplayName("POST /api/posts/{slug}/comments - blank content returns 400")
    @WithMockUser(username = "testuser", roles = "USER")
    void createComment_blankContent_returns400() throws Exception {
        CreateCommentRequest req = new CreateCommentRequest("   ");
        mockMvc.perform(post("/api/posts/test-post/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/comments/{id} - anonymous returns 401")
    void deleteComment_anonymous_returns401() throws Exception {
        mockMvc.perform(delete("/api/comments/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/comments/{id} - owner returns 204")
    @WithMockUser(username = "testuser", roles = "USER")
    void deleteComment_owner_returns204() throws Exception {
        mockMvc.perform(delete("/api/comments/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/comments/{id} - stranger returns 403")
    @WithMockUser(username = "stranger", roles = "USER")
    void deleteComment_stranger_returns403() throws Exception {
        doThrow(new ForbiddenOperationException("Not allowed"))
                .when(commentService).delete(eq(1L), any());

        mockMvc.perform(delete("/api/comments/1"))
                .andExpect(status().isForbidden());
    }
}
