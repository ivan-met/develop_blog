package met.ivan.devblog.repository;

import met.ivan.devblog.entity.Category;
import met.ivan.devblog.entity.Comment;
import met.ivan.devblog.entity.Post;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CommentRepository")
class CommentRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CommentRepository commentRepository;

    private User author;
    private Post post;

    @BeforeEach
    void setUp() {
        Role userRole = em.persistAndFlush(Role.builder().name(RoleName.USER).build());
        author = em.persistAndFlush(User.builder()
                .username("commentuser")
                .email("commentuser@test.com")
                .passwordHash("hash")
                .active(true)
                .roles(new HashSet<>(Set.of(userRole)))
                .build());

        Category cat = em.persistAndFlush(Category.builder()
                .name("Tech")
                .slug("tech")
                .build());

        post = em.persistAndFlush(Post.builder()
                .title("Test Post")
                .slug("test-post")
                .contentMarkdown("# Hello")
                .status(PostStatus.PUBLISHED)
                .author(author)
                .category(cat)
                .publishedAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("findByPostIdWithAuthor: returns comments for post in createdAt DESC order")
    void findByPostIdWithAuthor_orderedNewestFirst() throws InterruptedException {
        Comment older = em.persistAndFlush(Comment.builder()
                .content("First comment")
                .post(post)
                .author(author)
                .build());

        // Ensure distinct timestamps
        Thread.sleep(10);

        Comment newer = em.persistAndFlush(Comment.builder()
                .content("Second comment")
                .post(post)
                .author(author)
                .build());

        em.clear(); // force reload from DB

        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt")));
        Page<Comment> page = commentRepository.findByPostIdWithAuthor(post.getId(), pageable);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getContent()).isEqualTo("Second comment");
        assertThat(page.getContent().get(1).getContent()).isEqualTo("First comment");
    }

    @Test
    @DisplayName("findByPostIdWithAuthor: author is eagerly loaded (no lazy init after em.clear)")
    void findByPostIdWithAuthor_authorIsEagerlyLoaded() {
        em.persistAndFlush(Comment.builder()
                .content("A comment")
                .post(post)
                .author(author)
                .build());

        em.clear(); // detach all — lazy access would throw if not fetched

        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt")));
        Page<Comment> page = commentRepository.findByPostIdWithAuthor(post.getId(), pageable);

        assertThat(page.getContent()).hasSize(1);
        // Access author — must not throw LazyInitializationException
        String username = page.getContent().get(0).getAuthor().getUsername();
        assertThat(username).isEqualTo("commentuser");
    }

    @Test
    @DisplayName("findByPostIdWithAuthor: returns only comments for the requested post")
    void findByPostIdWithAuthor_isolatedByPost() {
        Category cat2 = em.persistAndFlush(Category.builder()
                .name("Other")
                .slug("other")
                .build());
        Post otherPost = em.persistAndFlush(Post.builder()
                .title("Other Post")
                .slug("other-post")
                .contentMarkdown("# Other")
                .status(PostStatus.PUBLISHED)
                .author(author)
                .category(cat2)
                .publishedAt(Instant.now())
                .build());

        em.persistAndFlush(Comment.builder()
                .content("Comment on post")
                .post(post)
                .author(author)
                .build());
        em.persistAndFlush(Comment.builder()
                .content("Comment on other post")
                .post(otherPost)
                .author(author)
                .build());

        em.clear();

        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt")));
        Page<Comment> page = commentRepository.findByPostIdWithAuthor(post.getId(), pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getContent()).isEqualTo("Comment on post");
    }

    @Test
    @DisplayName("findByPostIdWithAuthor: pagination works correctly")
    void findByPostIdWithAuthor_paginationWorks() {
        for (int i = 0; i < 5; i++) {
            em.persistAndFlush(Comment.builder()
                    .content("Comment " + i)
                    .post(post)
                    .author(author)
                    .build());
        }
        em.clear();

        PageRequest pageable = PageRequest.of(0, 3, Sort.by(Sort.Order.desc("createdAt")));
        Page<Comment> page = commentRepository.findByPostIdWithAuthor(post.getId(), pageable);

        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }
}
