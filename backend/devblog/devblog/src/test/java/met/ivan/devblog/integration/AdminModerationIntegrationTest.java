package met.ivan.devblog.integration;

import met.ivan.devblog.dto.AuthResponse;
import met.ivan.devblog.dto.LoginRequest;
import met.ivan.devblog.dto.PlatformStatsResponse;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.repository.CommentRepository;
import met.ivan.devblog.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Admin Moderation Integration Tests")
class AdminModerationIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

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

    // --- Comment moderation ---

    @Test
    @DisplayName("Admin can list all comments via GET /api/admin/comments (200)")
    void admin_canListAllComments() {
        String adminToken = loginToken("admin", "Admin@1234");

        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl() + "/api/admin/comments",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminToken)),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"content\"");
        assertThat(resp.getBody()).contains("\"totalElements\"");
        // Seeded data provides at least one comment
        assertThat(resp.getBody()).contains("\"postSlug\"");
        assertThat(resp.getBody()).contains("\"postTitle\"");
    }

    @Test
    @DisplayName("Admin can search comments via GET /api/admin/comments?search=... (200)")
    void admin_canSearchComments() {
        String adminToken = loginToken("admin", "Admin@1234");

        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl() + "/api/admin/comments?search=user",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminToken)),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\"");
    }

    @Test
    @DisplayName("USER role gets 403 on GET /api/admin/comments")
    void user_cannotListAdminComments() {
        String userToken = loginToken("user", "User@1234");

        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl() + "/api/admin/comments",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userToken)),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Anonymous gets 401 on GET /api/admin/comments")
    void anonymous_cannotListAdminComments() {
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl() + "/api/admin/comments",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- Content management ---

    @Test
    @DisplayName("Admin can list all posts (incl. drafts) via GET /api/admin/posts (200)")
    void admin_canListAllPosts() {
        String adminToken = loginToken("admin", "Admin@1234");

        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl() + "/api/admin/posts",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminToken)),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"totalElements\"");
        // Seeded data has published posts
        assertThat(resp.getBody()).contains("PUBLISHED");
    }

    @Test
    @DisplayName("Admin POST filter returns only PUBLISHED posts")
    void admin_filterByPublished() {
        String adminToken = loginToken("admin", "Admin@1234");

        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl() + "/api/admin/posts?status=PUBLISHED",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminToken)),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("PUBLISHED");
        assertThat(resp.getBody()).doesNotContain("DRAFT");
    }

    @Test
    @DisplayName("USER role gets 403 on GET /api/admin/posts")
    void user_cannotListAdminPosts() {
        String userToken = loginToken("user", "User@1234");

        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl() + "/api/admin/posts",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userToken)),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Anonymous gets 401 on GET /api/admin/posts")
    void anonymous_cannotListAdminPosts() {
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl() + "/api/admin/posts",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- Statistics ---

    @Test
    @DisplayName("Admin can fetch stats via GET /api/admin/stats (200) with non-zero seeded totals")
    void admin_canFetchStats() {
        String adminToken = loginToken("admin", "Admin@1234");

        ResponseEntity<PlatformStatsResponse> resp = restTemplate.exchange(
                baseUrl() + "/api/admin/stats",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminToken)),
                PlatformStatsResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        PlatformStatsResponse stats = resp.getBody();
        assertThat(stats).isNotNull();

        // Seeded data must have at least 2 users (admin + user)
        assertThat(stats.totals().users()).isGreaterThanOrEqualTo(2);
        assertThat(stats.totals().activeUsers()).isGreaterThanOrEqualTo(2);

        // Seeded data has published posts
        assertThat(stats.totals().publishedPosts()).isGreaterThan(0);

        // Seeded data has categories
        assertThat(stats.totals().categories()).isGreaterThan(0);

        // Response has the four top-level fields
        assertThat(stats.topPostsByViews()).isNotNull();
        assertThat(stats.topPostsByLikes()).isNotNull();
        assertThat(stats.recentUsers()).isNotNull();
    }

    @Test
    @DisplayName("Stats totals: publishedPosts + draftPosts == posts")
    void admin_statsTotalsConsistency() {
        String adminToken = loginToken("admin", "Admin@1234");

        ResponseEntity<PlatformStatsResponse> resp = restTemplate.exchange(
                baseUrl() + "/api/admin/stats",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminToken)),
                PlatformStatsResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        PlatformStatsResponse.StatsTotals t = resp.getBody().totals();
        assertThat(t.publishedPosts() + t.draftPosts()).isEqualTo(t.posts());
    }

    @Test
    @DisplayName("USER role gets 403 on GET /api/admin/stats")
    void user_cannotFetchStats() {
        String userToken = loginToken("user", "User@1234");

        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl() + "/api/admin/stats",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userToken)),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Anonymous gets 401 on GET /api/admin/stats")
    void anonymous_cannotFetchStats() {
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl() + "/api/admin/stats",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
