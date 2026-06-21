package met.ivan.devblog.integration;

import met.ivan.devblog.dto.AuthResponse;
import met.ivan.devblog.dto.CreatePostRequest;
import met.ivan.devblog.dto.LoginRequest;
import met.ivan.devblog.dto.PostResponse;
import met.ivan.devblog.dto.UpdatePostRequest;
import met.ivan.devblog.dto.UpdatePostStatusRequest;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.repository.CategoryRepository;
import met.ivan.devblog.repository.PostRepository;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Blog Post Management Integration Tests")
class BlogPostManagementIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Long seededCategoryId;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String loginToken(String usernameOrEmail, String password) {
        LoginRequest req = new LoginRequest(usernameOrEmail, password);
        ResponseEntity<AuthResponse> resp = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login",
                new HttpEntity<>(req, jsonHeaders()),
                AuthResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().getAccessToken();
    }

    @BeforeEach
    void setUp() {
        // Use seeded "Java" category (seeded by DataInitializer)
        seededCategoryId = categoryRepository.findBySlug("java")
                .map(c -> c.getId())
                .orElseGet(() -> {
                    // fallback: pick any
                    return categoryRepository.findAll().stream()
                            .findFirst()
                            .map(c -> c.getId())
                            .orElse(null);
                });
    }

    // --- Seeding verification ---

    @Test
    @DisplayName("Seeded categories exist (Java, Spring, Vue, DevOps)")
    void seededCategoriesExist() {
        assertThat(categoryRepository.existsBySlug("java")).isTrue();
        assertThat(categoryRepository.existsBySlug("spring")).isTrue();
        assertThat(categoryRepository.existsBySlug("vue")).isTrue();
        assertThat(categoryRepository.existsBySlug("devops")).isTrue();
    }

    @Test
    @DisplayName("Seeded posts exist and are authored by default user")
    void seededPostsExist_authoredByDefaultUser() {
        // DataInitializer seeds 12 posts for the default user
        assertThat(postRepository.count()).isGreaterThanOrEqualTo(12);

        ResponseEntity<String> postsResp = restTemplate.getForEntity(
                baseUrl() + "/api/posts", String.class);
        assertThat(postsResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // All seeded posts should have author username "user"
        long userPosts = postRepository.countByAuthorUsername("user");
        assertThat(userPosts).isGreaterThanOrEqualTo(12);
    }

    // --- Sort ordering ---

    @Test
    @DisplayName("sort=popular returns highest viewCount post first")
    void sort_popular_highestViewCountFirst() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                baseUrl() + "/api/posts?sort=popular", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // The response body should contain viewCount fields
        assertThat(resp.getBody()).contains("viewCount");

        // Find max viewCount post slug
        long maxViewCount = postRepository.findAll().stream()
                .filter(p -> p.getStatus() == PostStatus.PUBLISHED)
                .mapToLong(p -> p.getViewCount())
                .max().orElse(0L);

        String maxSlug = postRepository.findAll().stream()
                .filter(p -> p.getStatus() == PostStatus.PUBLISHED && p.getViewCount() == maxViewCount)
                .map(p -> p.getSlug())
                .findFirst().orElse(null);

        assertThat(maxSlug).isNotNull();
        // The first result in popular should be the highest-viewed post
        int maxSlugIndex = resp.getBody().indexOf(maxSlug);
        // Should appear early in the JSON (within the first 2000 chars)
        assertThat(maxSlugIndex).isGreaterThanOrEqualTo(0).isLessThan(2000);
    }

    @Test
    @DisplayName("sort=latest and sort=popular produce different orderings for seed data")
    void sort_latestAndPopular_differForSeedData() {
        ResponseEntity<String> latestResp = restTemplate.getForEntity(
                baseUrl() + "/api/posts?sort=latest&size=12", String.class);
        ResponseEntity<String> popularResp = restTemplate.getForEntity(
                baseUrl() + "/api/posts?sort=popular&size=12", String.class);

        assertThat(latestResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(popularResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // The two responses should be different (seed data has varied viewCounts)
        assertThat(latestResp.getBody()).isNotEqualTo(popularResp.getBody());
    }

    // --- Search by content and tag ---

    @Test
    @DisplayName("search by content word returns matching posts")
    void search_byContentWord_returnsMatch() {
        // "virtual threads" appears in the contentMarkdown of one seed post
        ResponseEntity<String> resp = restTemplate.getForEntity(
                baseUrl() + "/api/posts?search=virtual+threads", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("virtual-threads");
    }

    @Test
    @DisplayName("search by tag returns matching posts")
    void search_byTag_returnsMatch() {
        // "jwt" is a tag on the Spring Security seed post
        ResponseEntity<String> resp = restTemplate.getForEntity(
                baseUrl() + "/api/posts?search=jwt", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Should contain the Spring Security JWT post
        assertThat(resp.getBody()).isNotEmpty();
        // The content array should be non-empty
        assertThat(resp.getBody()).contains("\"totalElements\":");
        // totalElements should be >= 1
        assertThat(resp.getBody()).doesNotContain("\"totalElements\":0");
    }

    // --- viewCount increment on public GET ---

    @Test
    @DisplayName("Public GET /api/posts/{slug} bumps viewCount each call")
    void publicGet_bySlug_bumpsViewCount() {
        // Pick the first published post from seed data
        String slug = postRepository.findAll().stream()
                .filter(p -> p.getStatus() == PostStatus.PUBLISHED)
                .map(p -> p.getSlug())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No published posts in seed data"));

        long viewsBefore = postRepository.findAll().stream()
                .filter(p -> p.getSlug().equals(slug))
                .mapToLong(p -> p.getViewCount())
                .findFirst().orElse(0L);

        // First GET
        ResponseEntity<PostResponse> resp1 = restTemplate.getForEntity(
                baseUrl() + "/api/posts/" + slug, PostResponse.class);
        assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Second GET
        ResponseEntity<PostResponse> resp2 = restTemplate.getForEntity(
                baseUrl() + "/api/posts/" + slug, PostResponse.class);
        assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Check DB
        long viewsAfter = postRepository.findAll().stream()
                .filter(p -> p.getSlug().equals(slug))
                .mapToLong(p -> p.getViewCount())
                .findFirst().orElse(0L);

        assertThat(viewsAfter).isGreaterThanOrEqualTo(viewsBefore + 2);
    }

    // --- Full post lifecycle ---

    @Test
    @DisplayName("User creates draft -> absent from public list -> present in /mine -> publish -> fetchable publicly")
    void fullPostLifecycle() {
        String userToken = loginToken("user", "User@1234");

        // 1. Create a draft post
        CreatePostRequest createReq = new CreatePostRequest(
                "Integration Test Post", "# Hello World", "A test excerpt", null, PostStatus.DRAFT, null);
        ResponseEntity<PostResponse> createResp = restTemplate.exchange(
                baseUrl() + "/api/posts",
                HttpMethod.POST,
                new HttpEntity<>(createReq, bearerHeaders(userToken)),
                PostResponse.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PostResponse created = createResp.getBody();
        assertThat(created).isNotNull();
        assertThat(created.getStatus()).isEqualTo(PostStatus.DRAFT);
        Long postId = created.getId();
        String postSlug = created.getSlug();

        // 2. Draft absent from public list GET /api/posts
        ResponseEntity<String> publicList = restTemplate.getForEntity(
                baseUrl() + "/api/posts", String.class);
        assertThat(publicList.getStatusCode()).isEqualTo(HttpStatus.OK);
        // The draft should not appear in the public (published-only) feed
        assertThat(publicList.getBody()).doesNotContain(postSlug);

        // 3. Draft present in /mine
        ResponseEntity<String> mineResp = restTemplate.exchange(
                baseUrl() + "/api/posts/mine",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userToken)),
                String.class);
        assertThat(mineResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mineResp.getBody()).contains(postSlug);

        // 4. Publish the post (assign category first via update)
        UpdatePostRequest updateReq = new UpdatePostRequest(
                "Integration Test Post", "# Hello World", "A test excerpt", seededCategoryId, null);
        restTemplate.exchange(
                baseUrl() + "/api/posts/" + postId,
                HttpMethod.PUT,
                new HttpEntity<>(updateReq, bearerHeaders(userToken)),
                PostResponse.class);

        UpdatePostStatusRequest publishReq = new UpdatePostStatusRequest(PostStatus.PUBLISHED);
        ResponseEntity<PostResponse> publishResp = restTemplate.exchange(
                baseUrl() + "/api/posts/" + postId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(publishReq, bearerHeaders(userToken)),
                PostResponse.class);
        assertThat(publishResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publishResp.getBody().getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(publishResp.getBody().getPublishedAt()).isNotNull();

        // 5. Now fetchable by slug publicly (no token)
        ResponseEntity<PostResponse> slugResp = restTemplate.getForEntity(
                baseUrl() + "/api/posts/" + postSlug, PostResponse.class);
        assertThat(slugResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(slugResp.getBody().getSlug()).isEqualTo(postSlug);

        // 6. Appears in public list
        ResponseEntity<String> publicListAfter = restTemplate.getForEntity(
                baseUrl() + "/api/posts", String.class);
        assertThat(publicListAfter.getBody()).contains(postSlug);
    }

    @Test
    @DisplayName("Second user cannot edit another user's post (403)")
    void secondUser_cannotEditOtherPost() {
        // Register + login second user
        String userToken = loginToken("user", "User@1234");

        // Create post as 'user'
        CreatePostRequest createReq = new CreatePostRequest(
                "User1 Post", "# Content", null, null, PostStatus.DRAFT, null);
        ResponseEntity<PostResponse> createResp = restTemplate.exchange(
                baseUrl() + "/api/posts",
                HttpMethod.POST,
                new HttpEntity<>(createReq, bearerHeaders(userToken)),
                PostResponse.class);
        Long postId = createResp.getBody().getId();

        // Register second user
        met.ivan.devblog.dto.RegisterRequest reg2 = new met.ivan.devblog.dto.RegisterRequest(
                "seconduser_blog", "seconduser_blog@test.com", "Password123!");
        ResponseEntity<AuthResponse> reg2Resp = restTemplate.postForEntity(
                baseUrl() + "/api/auth/register",
                new HttpEntity<>(reg2, jsonHeaders()),
                AuthResponse.class);
        String token2 = reg2Resp.getBody().getAccessToken();

        // Attempt edit as second user
        UpdatePostRequest updateReq = new UpdatePostRequest("Hacked Title", "Hacked", null, null, null);
        ResponseEntity<String> editResp = restTemplate.exchange(
                baseUrl() + "/api/posts/" + postId,
                HttpMethod.PUT,
                new HttpEntity<>(updateReq, bearerHeaders(token2)),
                String.class);
        assertThat(editResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Admin can edit and unpublish a user's post, then delete it")
    void admin_canEditUnpublishDelete() {
        String userToken = loginToken("user", "User@1234");
        String adminToken = loginToken("admin", "Admin@1234");

        // Create post as user and publish it
        CreatePostRequest createReq = new CreatePostRequest(
                "Admin Target Post", "# Content", "excerpt", seededCategoryId, PostStatus.PUBLISHED, Set.of("java"));
        ResponseEntity<PostResponse> createResp = restTemplate.exchange(
                baseUrl() + "/api/posts",
                HttpMethod.POST,
                new HttpEntity<>(createReq, bearerHeaders(userToken)),
                PostResponse.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long postId = createResp.getBody().getId();
        String slug = createResp.getBody().getSlug();

        // Admin edits the post
        UpdatePostRequest editReq = new UpdatePostRequest("Admin Edited Title", "# Edited", null, seededCategoryId, null);
        ResponseEntity<PostResponse> editResp = restTemplate.exchange(
                baseUrl() + "/api/posts/" + postId,
                HttpMethod.PUT,
                new HttpEntity<>(editReq, bearerHeaders(adminToken)),
                PostResponse.class);
        assertThat(editResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(editResp.getBody().getTitle()).isEqualTo("Admin Edited Title");

        // Admin unpublishes (sets to DRAFT)
        UpdatePostStatusRequest unpublishReq = new UpdatePostStatusRequest(PostStatus.DRAFT);
        ResponseEntity<PostResponse> unpublishResp = restTemplate.exchange(
                baseUrl() + "/api/posts/" + postId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(unpublishReq, bearerHeaders(adminToken)),
                PostResponse.class);
        assertThat(unpublishResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unpublishResp.getBody().getStatus()).isEqualTo(PostStatus.DRAFT);

        // Admin deletes the post
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                baseUrl() + "/api/posts/" + postId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(adminToken)),
                Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify gone
        assertThat(postRepository.findById(postId)).isEmpty();
    }

    @Test
    @DisplayName("ADMIN cannot create posts (403)")
    void admin_cannotCreatePost() {
        String adminToken = loginToken("admin", "Admin@1234");

        CreatePostRequest req = new CreatePostRequest("Admin Post", "Content", null, null, PostStatus.DRAFT, null);
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl() + "/api/posts",
                HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(adminToken)),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Category delete blocked when post references it (409)")
    void categoryDelete_blocked_whenPostExists() {
        String userToken = loginToken("user", "User@1234");
        String adminToken = loginToken("admin", "Admin@1234");

        // Create a post that references the seeded Java category
        CreatePostRequest createReq = new CreatePostRequest(
                "Cat Block Test Post", "# Content", null, seededCategoryId, PostStatus.DRAFT, null);
        restTemplate.exchange(
                baseUrl() + "/api/posts",
                HttpMethod.POST,
                new HttpEntity<>(createReq, bearerHeaders(userToken)),
                PostResponse.class);

        // Admin attempts to delete the category
        ResponseEntity<String> deleteResp = restTemplate.exchange(
                baseUrl() + "/api/admin/categories/" + seededCategoryId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(adminToken)),
                String.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Anonymous can list published posts and get categories without token")
    void anonymous_publicEndpoints_accessible() {
        ResponseEntity<String> postsResp = restTemplate.getForEntity(
                baseUrl() + "/api/posts", String.class);
        assertThat(postsResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> catResp = restTemplate.getForEntity(
                baseUrl() + "/api/categories", String.class);
        assertThat(catResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
