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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("PostLikeRepository")
class PostLikeRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PostLikeRepository postLikeRepository;

    private User user;
    private Post post;
    private Post post2;

    @BeforeEach
    void setUp() {
        Role userRole = em.persistAndFlush(Role.builder().name(RoleName.USER).build());
        user = em.persistAndFlush(User.builder()
                .username("liker")
                .email("liker@test.com")
                .passwordHash("hash")
                .active(true)
                .roles(new HashSet<>(Set.of(userRole)))
                .build());

        Category cat = em.persistAndFlush(Category.builder()
                .name("Tech")
                .slug("tech-likes")
                .build());

        post = em.persistAndFlush(Post.builder()
                .title("Post 1")
                .slug("post-1-likes")
                .contentMarkdown("# P1")
                .status(PostStatus.PUBLISHED)
                .author(user)
                .category(cat)
                .publishedAt(Instant.now())
                .build());

        post2 = em.persistAndFlush(Post.builder()
                .title("Post 2")
                .slug("post-2-likes")
                .contentMarkdown("# P2")
                .status(PostStatus.PUBLISHED)
                .author(user)
                .category(cat)
                .publishedAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("existsByUserIdAndPostId: returns true when like exists")
    void existsByUserIdAndPostId_whenExists_returnsTrue() {
        em.persistAndFlush(PostLike.builder().user(user).post(post).build());
        assertThat(postLikeRepository.existsByUserIdAndPostId(user.getId(), post.getId())).isTrue();
    }

    @Test
    @DisplayName("existsByUserIdAndPostId: returns false when no like")
    void existsByUserIdAndPostId_whenAbsent_returnsFalse() {
        assertThat(postLikeRepository.existsByUserIdAndPostId(user.getId(), post.getId())).isFalse();
    }

    @Test
    @DisplayName("countByPostId: counts only likes for the specified post")
    void countByPostId_countsCorrectly() {
        em.persistAndFlush(PostLike.builder().user(user).post(post).build());
        // Another user likes post2
        User user2 = em.persistAndFlush(User.builder()
                .username("liker2")
                .email("liker2@test.com")
                .passwordHash("hash")
                .active(true)
                .roles(new HashSet<>())
                .build());
        em.persistAndFlush(PostLike.builder().user(user2).post(post).build());
        em.persistAndFlush(PostLike.builder().user(user2).post(post2).build());

        assertThat(postLikeRepository.countByPostId(post.getId())).isEqualTo(2);
        assertThat(postLikeRepository.countByPostId(post2.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("deleteByUserIdAndPostId: removes the like")
    void deleteByUserIdAndPostId_removesLike() {
        em.persistAndFlush(PostLike.builder().user(user).post(post).build());
        assertThat(postLikeRepository.existsByUserIdAndPostId(user.getId(), post.getId())).isTrue();

        postLikeRepository.deleteByUserIdAndPostId(user.getId(), post.getId());
        em.flush();

        assertThat(postLikeRepository.existsByUserIdAndPostId(user.getId(), post.getId())).isFalse();
    }

    @Test
    @DisplayName("unique constraint: duplicate like throws DataIntegrityViolationException")
    void duplicateLike_throwsConstraintViolation() {
        em.persistAndFlush(PostLike.builder().user(user).post(post).build());
        em.clear();

        assertThatThrownBy(() -> {
            postLikeRepository.saveAndFlush(PostLike.builder().user(user).post(post).build());
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("countsByPostIds: batch query returns correct counts per post")
    void countsByPostIds_batchCountsCorrect() {
        em.persistAndFlush(PostLike.builder().user(user).post(post).build());
        User user2 = em.persistAndFlush(User.builder()
                .username("liker3")
                .email("liker3@test.com")
                .passwordHash("hash")
                .active(true)
                .roles(new HashSet<>())
                .build());
        em.persistAndFlush(PostLike.builder().user(user2).post(post).build());
        em.persistAndFlush(PostLike.builder().user(user2).post(post2).build());
        em.flush();
        em.clear();

        List<Object[]> results = postLikeRepository.countsByPostIds(List.of(post.getId(), post2.getId()));
        assertThat(results).hasSize(2);

        // Map results for easier assertion
        long countPost1 = results.stream()
                .filter(row -> row[0].equals(post.getId()))
                .mapToLong(row -> (Long) row[1])
                .findFirst().orElse(0L);
        long countPost2 = results.stream()
                .filter(row -> row[0].equals(post2.getId()))
                .mapToLong(row -> (Long) row[1])
                .findFirst().orElse(0L);

        assertThat(countPost1).isEqualTo(2);
        assertThat(countPost2).isEqualTo(1);
    }

    @Test
    @DisplayName("findLikedPostIds: returns only post IDs liked by the given user")
    void findLikedPostIds_returnsCorrectIds() {
        em.persistAndFlush(PostLike.builder().user(user).post(post).build());
        em.flush();
        em.clear();

        List<Long> liked = postLikeRepository.findLikedPostIds(user.getId(), List.of(post.getId(), post2.getId()));
        assertThat(liked).containsExactly(post.getId());
    }
}
