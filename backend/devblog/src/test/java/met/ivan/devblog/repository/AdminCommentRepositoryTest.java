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
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for the admin global comment search query
 * {@link CommentRepository#findAllWithAuthorAndPost}.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CommentRepository - findAllWithAuthorAndPost")
class AdminCommentRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CommentRepository commentRepository;

    private User author1;
    private User author2;
    private Post post1;
    private Post post2;

    @BeforeEach
    void setUp() {
        Role userRole = em.persistAndFlush(Role.builder().name(RoleName.USER).build());

        author1 = em.persistAndFlush(User.builder()
                .username("alice")
                .email("alice@test.com")
                .passwordHash("hash")
                .displayName("Alice A")
                .active(true)
                .roles(new HashSet<>(Set.of(userRole)))
                .build());

        author2 = em.persistAndFlush(User.builder()
                .username("bob")
                .email("bob@test.com")
                .passwordHash("hash")
                .displayName("Bob B")
                .active(true)
                .roles(new HashSet<>(Set.of(userRole)))
                .build());

        Category cat = em.persistAndFlush(Category.builder()
                .name("Tech")
                .slug("tech")
                .build());

        post1 = em.persistAndFlush(Post.builder()
                .title("Spring Boot Guide")
                .slug("spring-boot-guide")
                .contentMarkdown("# Spring Boot")
                .status(PostStatus.PUBLISHED)
                .author(author1)
                .category(cat)
                .publishedAt(Instant.now())
                .build());

        post2 = em.persistAndFlush(Post.builder()
                .title("Vue 3 Tutorial")
                .slug("vue-3-tutorial")
                .contentMarkdown("# Vue")
                .status(PostStatus.PUBLISHED)
                .author(author2)
                .category(cat)
                .publishedAt(Instant.now())
                .build());
    }

    private PageRequest defaultPageable() {
        return PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
    }

    @Test
    @DisplayName("returns all comments when search is null")
    void findAll_noSearch_returnsAll() {
        em.persistAndFlush(Comment.builder().content("Hello world").post(post1).author(author1).build());
        em.persistAndFlush(Comment.builder().content("Vue is great").post(post2).author(author2).build());
        em.clear();

        Page<Comment> page = commentRepository.findAllWithAuthorAndPost(null, defaultPageable());

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("matches comment content (case-insensitive)")
    void findAll_searchByContent_caseInsensitive() {
        em.persistAndFlush(Comment.builder().content("Spring is amazing").post(post1).author(author1).build());
        em.persistAndFlush(Comment.builder().content("Vue rocks").post(post2).author(author2).build());
        em.clear();

        Page<Comment> page = commentRepository.findAllWithAuthorAndPost("SPRING", defaultPageable());

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getContent()).isEqualTo("Spring is amazing");
    }

    @Test
    @DisplayName("matches author username (case-insensitive)")
    void findAll_searchByAuthorUsername_caseInsensitive() {
        em.persistAndFlush(Comment.builder().content("Alice's comment").post(post1).author(author1).build());
        em.persistAndFlush(Comment.builder().content("Bob's comment").post(post2).author(author2).build());
        em.clear();

        Page<Comment> page = commentRepository.findAllWithAuthorAndPost("ALICE", defaultPageable());

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getAuthor().getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("matches post title (case-insensitive)")
    void findAll_searchByPostTitle_caseInsensitive() {
        em.persistAndFlush(Comment.builder().content("Comment on spring post").post(post1).author(author1).build());
        em.persistAndFlush(Comment.builder().content("Comment on vue post").post(post2).author(author2).build());
        em.clear();

        Page<Comment> page = commentRepository.findAllWithAuthorAndPost("vue 3", defaultPageable());

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getPost().getTitle()).isEqualTo("Vue 3 Tutorial");
    }

    @Test
    @DisplayName("returns empty when search matches nothing")
    void findAll_noMatch_returnsEmpty() {
        em.persistAndFlush(Comment.builder().content("Hello world").post(post1).author(author1).build());
        em.clear();

        Page<Comment> page = commentRepository.findAllWithAuthorAndPost("xyzzy-no-match", defaultPageable());

        assertThat(page.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("author is eagerly loaded (no lazy init after em.clear)")
    void findAll_authorIsEagerlyLoaded() {
        em.persistAndFlush(Comment.builder().content("Eager test").post(post1).author(author1).build());
        em.clear();

        Page<Comment> page = commentRepository.findAllWithAuthorAndPost(null, defaultPageable());

        assertThat(page.getContent()).hasSize(1);
        // Accessing author after clear must not throw LazyInitializationException
        assertThat(page.getContent().get(0).getAuthor().getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("post and post author are eagerly loaded (no lazy init after em.clear)")
    void findAll_postIsEagerlyLoaded() {
        em.persistAndFlush(Comment.builder().content("Post eager test").post(post1).author(author1).build());
        em.clear();

        Page<Comment> page = commentRepository.findAllWithAuthorAndPost(null, defaultPageable());

        // Access post title and post author — must not throw
        Comment c = page.getContent().get(0);
        assertThat(c.getPost().getTitle()).isEqualTo("Spring Boot Guide");
        assertThat(c.getPost().getAuthor().getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("pagination works correctly")
    void findAll_paginationWorks() {
        for (int i = 0; i < 5; i++) {
            em.persistAndFlush(Comment.builder()
                    .content("Comment " + i)
                    .post(post1)
                    .author(author1)
                    .build());
        }
        em.clear();

        PageRequest pageable = PageRequest.of(0, 3, Sort.by(Sort.Order.desc("createdAt")));
        Page<Comment> page = commentRepository.findAllWithAuthorAndPost(null, pageable);

        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }
}
