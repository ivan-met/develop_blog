package met.ivan.devblog.controller;

import tools.jackson.databind.ObjectMapper;
import met.ivan.devblog.config.AppProperties;
import met.ivan.devblog.config.SecurityConfig;
import met.ivan.devblog.dto.AuthorProfileResponse;
import met.ivan.devblog.dto.AuthorSummary;
import met.ivan.devblog.dto.CategoryResponse;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.exception.ResourceNotFoundException;
import met.ivan.devblog.security.CustomUserDetailsService;
import met.ivan.devblog.security.JwtAuthenticationFilter;
import met.ivan.devblog.security.JwtService;
import met.ivan.devblog.service.AuthorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthorController.class, GlobalExceptionHandler.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ActiveProfiles("test")
@DisplayName("AuthorController")
class AuthorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorService authorService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private AuthorProfileResponse sampleProfile() {
        return new AuthorProfileResponse("testuser", "Test User", "Bio here", null,
                Instant.now(), 12L);
    }

    private PostSummaryResponse sampleSummary() {
        return new PostSummaryResponse(10L, "test-post", "Test Post", "Excerpt",
                PostStatus.PUBLISHED,
                new CategoryResponse(1L, "Java", "java", null),
                new AuthorSummary(1L, "testuser", "Test User"),
                Instant.now(), Instant.now(), Set.of("java"), 42L, 5L);
    }

    // --- GET /api/authors/{username} ---

    @Test
    @DisplayName("GET /api/authors/{username} - anonymous returns 200 (public)")
    void getProfile_anonymous_returns200() throws Exception {
        when(authorService.getPublicProfile("testuser")).thenReturn(sampleProfile());

        mockMvc.perform(get("/api/authors/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.postCount").value(12));
    }

    @Test
    @DisplayName("GET /api/authors/{username} - response does not contain email, roles, id, or active")
    void getProfile_responseOmitsSensitiveFields() throws Exception {
        when(authorService.getPublicProfile("testuser")).thenReturn(sampleProfile());

        mockMvc.perform(get("/api/authors/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.roles").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.active").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/authors/{username} - unknown user returns 404")
    void getProfile_unknownUser_returns404() throws Exception {
        when(authorService.getPublicProfile("nobody"))
                .thenThrow(new ResourceNotFoundException("Author not found"));

        mockMvc.perform(get("/api/authors/nobody"))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/authors/{username}/posts ---

    @Test
    @DisplayName("GET /api/authors/{username}/posts - anonymous returns 200 (public)")
    void getAuthorPosts_anonymous_returns200() throws Exception {
        Page<PostSummaryResponse> page = new PageImpl<>(List.of(sampleSummary()), PageRequest.of(0, 20), 1);
        when(authorService.getPublishedPosts(eq("testuser"), any())).thenReturn(page);

        mockMvc.perform(get("/api/authors/testuser/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("test-post"));
    }

    @Test
    @DisplayName("GET /api/authors/{username}/posts - unknown author returns 404")
    void getAuthorPosts_unknownAuthor_returns404() throws Exception {
        when(authorService.getPublishedPosts(eq("nobody"), any()))
                .thenThrow(new ResourceNotFoundException("Author not found"));

        mockMvc.perform(get("/api/authors/nobody/posts"))
                .andExpect(status().isNotFound());
    }
}
