package met.ivan.devblog.repository;

import met.ivan.devblog.entity.Category;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostBookmark;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("PostBookmarkRepository")
class PostBookmarkRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PostBookmarkRepository postBookmarkRepository;

    private User user;
    private Post post;
    private Post post2;

    @BeforeEach
    void setUp() {
        Role userRole = em.persistAndFlush(Role.builder().name(RoleName.USER).build());
        user = em.persistAndFlush(User.builder()
                .username("bookmarker")
                .email("bookmarker@test.com")
                .passwordHash("hash")
                .active(true)
                .roles(new HashSet<>(Set.of(userRole)))
                .build());

        Category cat = em.persistAndFlush(Category.builder()
                .name("Dev")
                .slug("dev-bookmarks")
                .build());

        post = em.persistAndFlush(Post.builder()
                .title("BM Post 1")
                .slug("bm-post-1")
                .contentMarkdown("# BM1")
                .status(PostStatus.PUBLISHED)
                .author(user)
                .category(cat)
                .publishedAt(Instant.now())
                .build());

        post2 = em.persistAndFlush(Post.builder()
                .title("BM Post 2")
                .slug("bm-post-2")
                .contentMarkdown("# BM2")
                .status(PostStatus.PUBLISHED)
                .author(user)
                .category(cat)
                .publishedAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("existsByUserIdAndPostId: returns true when bookmark exists")
    void existsByUserIdAndPostId_whenExists_returnsTrue() {
        em.persistAndFlush(PostBookmark.builder().user(user).post(post).build());
        assertThat(postBookmarkRepository.existsByUserIdAndPostId(user.getId(), post.getId())).isTrue();
    }

    @Test
    @DisplayName("existsByUserIdAndPostId: returns false when no bookmark")
    void existsByUserIdAndPostId_whenAbsent_returnsFalse() {
        assertThat(postBookmarkRepository.existsByUserIdAndPostId(user.getId(), post.getId())).isFalse();
    }

    @Test
    @DisplayName("deleteByUserIdAndPostId: removes the bookmark")
    void deleteByUserIdAndPostId_removesBookmark() {
        em.persistAndFlush(PostBookmark.builder().user(user).post(post).build());
        assertThat(postBookmarkRepository.existsByUserIdAndPostId(user.getId(), post.getId())).isTrue();

        postBookmarkRepository.deleteByUserIdAndPostId(user.getId(), post.getId());
        em.flush();

        assertThat(postBookmarkRepository.existsByUserIdAndPostId(user.getId(), post.getId())).isFalse();
    }

    @Test
    @DisplayName("unique constraint: duplicate bookmark throws DataIntegrityViolationException")
    void duplicateBookmark_throwsConstraintViolation() {
        em.persistAndFlush(PostBookmark.builder().user(user).post(post).build());
        em.clear();

        assertThatThrownBy(() -> {
            postBookmarkRepository.saveAndFlush(PostBookmark.builder().user(user).post(post).build());
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("findBookmarkedPostsByUserId: returns bookmarked posts for user")
    void findBookmarkedPostsByUserId_returnsPosts() {
        em.persistAndFlush(PostBookmark.builder().user(user).post(post).build());
        em.persistAndFlush(PostBookmark.builder().user(user).post(post2).build());
        em.clear();

        Page<Post> result = postBookmarkRepository.findBookmarkedPostsByUserId(
                user.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("findBookmarkedPostsByUserId: post author is eagerly loaded after em.clear")
    void findBookmarkedPostsByUserId_authorEagerlyLoaded() {
        em.persistAndFlush(PostBookmark.builder().user(user).post(post).build());
        em.clear();

        Page<Post> result = postBookmarkRepository.findBookmarkedPostsByUserId(
                user.getId(), PageRequest.of(0, 10));

        // Must not throw LazyInitializationException
        String authorUsername = result.getContent().get(0).getAuthor().getUsername();
        assertThat(authorUsername).isEqualTo("bookmarker");
    }

    @Test
    @DisplayName("findBookmarkedPostsByUserId: isolated to requesting user")
    void findBookmarkedPostsByUserId_isolatedToUser() {
        User otherUser = em.persistAndFlush(User.builder()
                .username("otheruser")
                .email("other@test.com")
                .passwordHash("hash")
                .active(true)
                .roles(new HashSet<>())
                .build());

        em.persistAndFlush(PostBookmark.builder().user(user).post(post).build());
        em.persistAndFlush(PostBookmark.builder().user(otherUser).post(post2).build());
        em.clear();

        Page<Post> result = postBookmarkRepository.findBookmarkedPostsByUserId(
                user.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getSlug()).isEqualTo("bm-post-1");
    }
}
