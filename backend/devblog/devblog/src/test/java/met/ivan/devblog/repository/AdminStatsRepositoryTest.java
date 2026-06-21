package met.ivan.devblog.repository;

import met.ivan.devblog.entity.Category;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostLike;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.entity.Role;
import met.ivan.devblog.entity.RoleName;
import met.ivan.devblog.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for the new stats aggregate queries:
 * - {@link UserRepository#countByActiveTrue()}
 * - {@link UserRepository#findTop5ByOrderByCreatedAtDesc(PageRequest)}
 * - {@link PostRepository#countByStatus(PostStatus)}
 * - {@link PostRepository#findTopByStatusOrderByViewCountDesc(PostStatus, PageRequest)}
 * - {@link PostLikeRepository#findTopPostsByLikeCount(PageRequest)}
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Stats aggregate repository queries")
class AdminStatsRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    private Role userRole;
    private Category category;

    @BeforeEach
    void setUp() {
        userRole = em.persistAndFlush(Role.builder().name(RoleName.USER).build());
        category = em.persistAndFlush(Category.builder()
                .name("Tech")
                .slug("tech")
                .build());
    }

    private User persistUser(String username, boolean active) {
        return em.persistAndFlush(User.builder()
                .username(username)
                .email(username + "@test.com")
                .passwordHash("hash")
                .active(active)
                .roles(new HashSet<>(Set.of(userRole)))
                .build());
    }

    private Post persistPost(String slug, PostStatus status, long viewCount, User author) {
        return em.persistAndFlush(Post.builder()
                .title("Post " + slug)
                .slug(slug)
                .contentMarkdown("Content")
                .status(status)
                .author(author)
                .category(category)
                .viewCount(viewCount)
                .publishedAt(status == PostStatus.PUBLISHED ? Instant.now() : null)
                .build());
    }

    // --- UserRepository ---

    @Test
    @DisplayName("countByActiveTrue: counts only active users")
    void countByActiveTrue_countsOnlyActive() {
        persistUser("active1", true);
        persistUser("active2", true);
        persistUser("inactive1", false);

        long count = userRepository.countByActiveTrue();

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("findTop5ByOrderByCreatedAtDesc: returns users newest-first, limited to 5")
    void findTop5ByOrderByCreatedAtDesc_newestFirst() throws InterruptedException {
        for (int i = 1; i <= 6; i++) {
            persistUser("user" + i, true);
            Thread.sleep(5); // ensure distinct timestamps
        }
        em.clear();

        List<User> recent = userRepository.findTop5ByOrderByCreatedAtDesc(PageRequest.of(0, 5));

        assertThat(recent).hasSize(5);
        // Newest first — user6 is the most recently created
        assertThat(recent.get(0).getUsername()).isEqualTo("user6");
    }

    // --- PostRepository ---

    @Test
    @DisplayName("countByStatus: counts only posts with matching status")
    void countByStatus_countsCorrectly() {
        User author = persistUser("postauthor", true);
        persistPost("pub1", PostStatus.PUBLISHED, 10L, author);
        persistPost("pub2", PostStatus.PUBLISHED, 20L, author);
        persistPost("draft1", PostStatus.DRAFT, 0L, author);

        long published = postRepository.countByStatus(PostStatus.PUBLISHED);
        long draft = postRepository.countByStatus(PostStatus.DRAFT);

        assertThat(published).isEqualTo(2);
        assertThat(draft).isEqualTo(1);
    }

    @Test
    @DisplayName("findTopByStatusOrderByViewCountDesc: returns top N PUBLISHED by viewCount")
    void findTopByStatus_returnsHighestViewCountFirst() {
        User author = persistUser("topauthor", true);
        persistPost("low", PostStatus.PUBLISHED, 10L, author);
        persistPost("high", PostStatus.PUBLISHED, 500L, author);
        persistPost("mid", PostStatus.PUBLISHED, 100L, author);
        persistPost("draft", PostStatus.DRAFT, 999L, author); // draft — should be excluded
        em.clear();

        List<Post> top = postRepository.findTopByStatusOrderByViewCountDesc(
                PostStatus.PUBLISHED, PageRequest.of(0, 5));

        assertThat(top).hasSize(3);
        assertThat(top.get(0).getSlug()).isEqualTo("high");
        assertThat(top.get(1).getSlug()).isEqualTo("mid");
        assertThat(top.get(2).getSlug()).isEqualTo("low");
        // Draft post is excluded
        assertThat(top.stream().noneMatch(p -> p.getStatus() == PostStatus.DRAFT)).isTrue();
    }

    @Test
    @DisplayName("findTopByStatusOrderByViewCountDesc: respects Pageable limit")
    void findTopByStatus_respectsPageableLimit() {
        User author = persistUser("limitauthor", true);
        for (int i = 1; i <= 7; i++) {
            persistPost("post-" + i, PostStatus.PUBLISHED, i * 10L, author);
        }
        em.clear();

        List<Post> top5 = postRepository.findTopByStatusOrderByViewCountDesc(
                PostStatus.PUBLISHED, PageRequest.of(0, 5));

        assertThat(top5).hasSize(5);
    }

    // --- PostLikeRepository ---

    @Test
    @DisplayName("findTopPostsByLikeCount: returns posts with most likes first")
    void findTopPostsByLikeCount_mostLikedFirst() {
        User author = persistUser("likeauthor", true);
        User liker1 = persistUser("liker1", true);
        User liker2 = persistUser("liker2", true);
        User liker3 = persistUser("liker3", true);

        Post popular = persistPost("popular-post", PostStatus.PUBLISHED, 0L, author);
        Post moderate = persistPost("moderate-post", PostStatus.PUBLISHED, 0L, author);
        Post unpopular = persistPost("unpopular-post", PostStatus.PUBLISHED, 0L, author);

        // popular: 3 likes, moderate: 2 likes, unpopular: 1 like
        em.persistAndFlush(PostLike.builder().user(liker1).post(popular).build());
        em.persistAndFlush(PostLike.builder().user(liker2).post(popular).build());
        em.persistAndFlush(PostLike.builder().user(liker3).post(popular).build());
        em.persistAndFlush(PostLike.builder().user(liker1).post(moderate).build());
        em.persistAndFlush(PostLike.builder().user(liker2).post(moderate).build());
        em.persistAndFlush(PostLike.builder().user(liker1).post(unpopular).build());
        em.clear();

        List<Object[]> rows = postLikeRepository.findTopPostsByLikeCount(PageRequest.of(0, 5));

        assertThat(rows).hasSize(3);
        assertThat(((Post) rows.get(0)[0]).getSlug()).isEqualTo("popular-post");
        assertThat((Long) rows.get(0)[1]).isEqualTo(3L);
        assertThat(((Post) rows.get(1)[0]).getSlug()).isEqualTo("moderate-post");
        assertThat((Long) rows.get(1)[1]).isEqualTo(2L);
    }

    @Test
    @DisplayName("findTopPostsByLikeCount: post author is accessible within the same transaction")
    void findTopPostsByLikeCount_authorAccessibleInTransaction() {
        User author = persistUser("eagerauthor", true);
        User liker = persistUser("eagerliker", true);
        Post post = persistPost("eager-post", PostStatus.PUBLISHED, 0L, author);
        em.persistAndFlush(PostLike.builder().user(liker).post(post).build());
        em.clear();

        // findTopPostsByLikeCount runs within the @DataJpaTest transaction,
        // so lazy associations can be loaded within the same session
        List<Object[]> rows = postLikeRepository.findTopPostsByLikeCount(PageRequest.of(0, 5));

        assertThat(rows).hasSize(1);
        Post result = (Post) rows.get(0)[0];
        assertThat(result.getAuthor().getUsername()).isEqualTo("eagerauthor");
    }

    @Test
    @DisplayName("findTopPostsByLikeCount: respects Pageable limit")
    void findTopPostsByLikeCount_respectsPageableLimit() {
        User author = persistUser("plimitauthor", true);
        User liker = persistUser("plimitliker", true);
        for (int i = 1; i <= 7; i++) {
            Post p = persistPost("liked-post-" + i, PostStatus.PUBLISHED, 0L, author);
            em.persistAndFlush(PostLike.builder().user(liker).post(p).build());
        }
        em.clear();

        List<Object[]> rows = postLikeRepository.findTopPostsByLikeCount(PageRequest.of(0, 5));

        assertThat(rows).hasSize(5);
    }

    @Test
    @DisplayName("findTopPostsByLikeCount: returns empty when no likes exist")
    void findTopPostsByLikeCount_empty_returnsEmpty() {
        List<Object[]> rows = postLikeRepository.findTopPostsByLikeCount(PageRequest.of(0, 5));

        assertThat(rows).isEmpty();
    }
}
