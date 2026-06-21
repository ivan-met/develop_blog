package met.ivan.devblog.integration;

import met.ivan.devblog.dto.AuthResponse;
import met.ivan.devblog.dto.AuthorProfileResponse;
import met.ivan.devblog.dto.BookmarkResponse;
import met.ivan.devblog.dto.CommentResponse;
import met.ivan.devblog.dto.CreateCommentRequest;
import met.ivan.devblog.dto.LikeResponse;
import met.ivan.devblog.dto.LoginRequest;
import met.ivan.devblog.dto.PostResponse;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.repository.CommentRepository;
import met.ivan.devblog.repository.PostLikeRepository;
import met.ivan.devblog.repository.PostRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Engagement Integration Tests")
class EngagementIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders h = jsonHeaders();
        h.setBearerAuth(token);
        return h;
    }

    private String loginToken(String username, String password) {
        LoginRequest req = new LoginRequest(username, password);
        ResponseEntity<AuthResponse> resp = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login",
                new HttpEntity<>(req, jsonHeaders()),
                AuthResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().getAccessToken();
    }

    private String firstPublishedSlug() {
        return postRepository.findAll().stream()
                .filter(p -> p.getStatus() == PostStatus.PUBLISHED)
                .map(p -> p.getSlug())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No published posts in seed data"));
    }

    // --- Comment lifecycle ---

    @Test
    @DisplayName("Comment lifecycle: create → list (public) → canDelete=true for author → delete → gone")
    void commentLifecycle() {
        String userToken = loginToken("user", "User@1234");
        String slug = firstPublishedSlug();

        // 1. Anonymous can list comments
        ResponseEntity<String> anonList = restTemplate.getForEntity(
                baseUrl() + "/api/posts/" + slug + "/comments", String.class);
        assertThat(anonList.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 2. Create a comment as 'user'
        CreateCommentRequest createReq = new CreateCommentRequest("Integration test comment!");
        ResponseEntity<CommentResponse> createResp = restTemplate.exchange(
                baseUrl() + "/api/posts/" + slug + "/comments",
                HttpMethod.POST,
                new HttpEntity<>(createReq, bearerHeaders(userToken)),
                CommentResponse.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        CommentResponse created = createResp.getBody();
        assertThat(created).isNotNull();
        assertThat(created.getContent()).isEqualTo("Integration test comment!");
        assertThat(created.isCanDelete()).isTrue(); // author gets canDelete=true
        Long commentId = created.getId();

        // 3. Anonymous cannot post a comment (401)
        ResponseEntity<String> anonPost = restTemplate.postForEntity(
                baseUrl() + "/api/posts/" + slug + "/comments",
                new HttpEntity<>(createReq, jsonHeaders()),
                String.class);
        assertThat(anonPost.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 4. Delete the comment
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                baseUrl() + "/api/comments/" + commentId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(userToken)),
                Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 5. Comment is gone from DB
        assertThat(commentRepository.findById(commentId)).isEmpty();
    }

    @Test
    @DisplayName("Comment delete: stranger cannot delete another user's comment (403)")
    void commentDelete_stranger_forbidden() {
        String userToken = loginToken("user", "User@1234");
        String slug = firstPublishedSlug();

        // Create comment as 'user'
        CreateCommentRequest req = new CreateCommentRequest("Only I can delete this");
        ResponseEntity<CommentResponse> createResp = restTemplate.exchange(
                baseUrl() + "/api/posts/" + slug + "/comments",
                HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(userToken)),
                CommentResponse.class);
        Long commentId = createResp.getBody().getId();

        // Register a new user and try to delete
        met.ivan.devblog.dto.RegisterRequest reg = new met.ivan.devblog.dto.RegisterRequest(
                "strangereng", "strangereng@test.com", "Password123!");
        ResponseEntity<AuthResponse> regResp = restTemplate.postForEntity(
                baseUrl() + "/api/auth/register",
                new HttpEntity<>(reg, jsonHeaders()),
                AuthResponse.class);
        String strangerToken = regResp.getBody().getAccessToken();

        ResponseEntity<String> forbidden = restTemplate.exchange(
                baseUrl() + "/api/comments/" + commentId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(strangerToken)),
                String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // --- Like idempotency ---

    @Test
    @DisplayName("Like: toggle idempotent — likeCount reflected in PostResponse")
    void likeIdempotencyAndCountInPostResponse() {
        String userToken = loginToken("user", "User@1234");
        String slug = firstPublishedSlug();

        // Like the post
        ResponseEntity<LikeResponse> likeResp = restTemplate.exchange(
                baseUrl() + "/api/posts/" + slug + "/like",
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(userToken)),
                LikeResponse.class);
        assertThat(likeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(likeResp.getBody().isLiked()).isTrue();
        long countAfterLike = likeResp.getBody().getLikeCount();
        assertThat(countAfterLike).isGreaterThan(0);

        // Like again — idempotent, count stays same
        ResponseEntity<LikeResponse> likeAgain = restTemplate.exchange(
                baseUrl() + "/api/posts/" + slug + "/like",
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(userToken)),
                LikeResponse.class);
        assertThat(likeAgain.getBody().getLikeCount()).isEqualTo(countAfterLike);
        assertThat(likeAgain.getBody().isLiked()).isTrue();

        // Unlike
        ResponseEntity<LikeResponse> unlikeResp = restTemplate.exchange(
                baseUrl() + "/api/posts/" + slug + "/like",
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(userToken)),
                LikeResponse.class);
        assertThat(unlikeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unlikeResp.getBody().isLiked()).isFalse();
        assertThat(unlikeResp.getBody().getLikeCount()).isEqualTo(countAfterLike - 1);

        // Unlike again — idempotent
        ResponseEntity<LikeResponse> unlikeAgain = restTemplate.exchange(
                baseUrl() + "/api/posts/" + slug + "/like",
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(userToken)),
                LikeResponse.class);
        assertThat(unlikeAgain.getBody().getLikeCount()).isEqualTo(countAfterLike - 1);
        assertThat(unlikeAgain.getBody().isLiked()).isFalse();
    }

    @Test
    @DisplayName("Like: anonymous cannot like (401)")
    void like_anonymous_unauthorized() {
        String slug = firstPublishedSlug();
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl() + "/api/posts/" + slug + "/like",
                HttpMethod.POST,
                new HttpEntity<>(jsonHeaders()),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- Bookmark lifecycle ---

    @Test
    @DisplayName("Bookmark: add → appears in /me/bookmarks → remove → gone from list")
    void bookmarkLifecycle() {
        String userToken = loginToken("user", "User@1234");
        String slug = firstPublishedSlug();

        // Bookmark the post
        ResponseEntity<BookmarkResponse> bookmarkResp = restTemplate.exchange(
                baseUrl() + "/api/posts/" + slug + "/bookmark",
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(userToken)),
                BookmarkResponse.class);
        assertThat(bookmarkResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bookmarkResp.getBody().isBookmarked()).isTrue();

        // Verify it appears in /api/users/me/bookmarks
        ResponseEntity<String> bookmarksResp = restTemplate.exchange(
                baseUrl() + "/api/users/me/bookmarks",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userToken)),
                String.class);
        assertThat(bookmarksResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bookmarksResp.getBody()).contains(slug);

        // Remove bookmark
        ResponseEntity<BookmarkResponse> removeResp = restTemplate.exchange(
                baseUrl() + "/api/posts/" + slug + "/bookmark",
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(userToken)),
                BookmarkResponse.class);
        assertThat(removeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(removeResp.getBody().isBookmarked()).isFalse();
    }

    @Test
    @DisplayName("Bookmarks: anonymous cannot access /api/users/me/bookmarks (401)")
    void listBookmarks_anonymous_unauthorized() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                baseUrl() + "/api/users/me/bookmarks", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- Author profile ---

    @Test
    @DisplayName("GET /api/authors/{username} returns profile without email/roles/id")
    void authorProfile_omitsSensitiveFields() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                baseUrl() + "/api/authors/user", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"username\"");
        assertThat(resp.getBody()).doesNotContain("\"email\"");
        assertThat(resp.getBody()).doesNotContain("\"roles\"");
        assertThat(resp.getBody()).doesNotContain("\"active\"");
        // id field should not appear
        assertThat(resp.getBody()).doesNotContain("\"id\"");
    }

    @Test
    @DisplayName("GET /api/authors/{username} is public — no token required")
    void authorProfile_public() {
        ResponseEntity<AuthorProfileResponse> resp = restTemplate.getForEntity(
                baseUrl() + "/api/authors/user", AuthorProfileResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getUsername()).isEqualTo("user");
        assertThat(resp.getBody().getPostCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("GET /api/authors/{username} - unknown user returns 404")
    void authorProfile_unknownUser_returns404() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                baseUrl() + "/api/authors/doesnotexist", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("GET /api/authors/{username}/posts returns only PUBLISHED posts")
    void authorPosts_onlyPublished() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                baseUrl() + "/api/authors/user/posts", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // All seed posts are PUBLISHED
        assertThat(resp.getBody()).contains("\"totalElements\"");
        assertThat(resp.getBody()).doesNotContain("\"DRAFT\"");
    }

    @Test
    @DisplayName("PostResponse includes likeCount when fetching by slug (anonymous → no liked/bookmarked)")
    void postResponse_includesLikeCount() {
        String slug = firstPublishedSlug();
        ResponseEntity<PostResponse> resp = restTemplate.getForEntity(
                baseUrl() + "/api/posts/" + slug, PostResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getLikeCount()).isNotNull();
        // Anonymous user: liked and bookmarked should be null (omitted by @JsonInclude NON_NULL)
    }
}
