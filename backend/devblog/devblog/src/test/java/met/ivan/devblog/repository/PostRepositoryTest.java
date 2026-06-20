package met.ivan.devblog.repository;

import met.ivan.devblog.entity.Category;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("PostRepository")
class PostRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PostRepository postRepository;

    private User author;
    private Category category;

    @BeforeEach
    void setUp() {
        Role userRole = em.persistAndFlush(Role.builder().name(RoleName.USER).build());
        author = em.persistAndFlush(User.builder()
                .username("postauthor")
                .email("postauthor@test.com")
                .passwordHash("hash")
                .active(true)
                .roles(new HashSet<>(Set.of(userRole)))
                .build());
        category = em.persistAndFlush(Category.builder()
                .name("Java")
                .slug("java")
                .description("Java desc")
                .build());
    }

    private Post savePost(String title, String slug, PostStatus status, Category cat) {
        Post p = Post.builder()
                .title(title)
                .slug(slug)
                .contentMarkdown("Content for " + title)
                .status(status)
                .author(author)
                .category(cat)
                .build();
        return em.persistAndFlush(p);
    }

    @Test
    @DisplayName("findBySlug: loads author and category via JOIN FETCH")
    void findBySlug_loadsRelations() {
        savePost("Java Basics", "java-basics", PostStatus.PUBLISHED, category);
        em.clear();

        Optional<Post> result = postRepository.findBySlug("java-basics");

        assertThat(result).isPresent();
        assertThat(result.get().getAuthor().getUsername()).isEqualTo("postauthor");
        assertThat(result.get().getCategory().getName()).isEqualTo("Java");
    }

    @Test
    @DisplayName("findBySlugAndStatus: returns post for matching status")
    void findBySlugAndStatus_matchingStatus() {
        savePost("Published Post", "published-post", PostStatus.PUBLISHED, category);
        em.clear();

        Optional<Post> result = postRepository.findBySlugAndStatus("published-post", PostStatus.PUBLISHED);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(PostStatus.PUBLISHED);
    }

    @Test
    @DisplayName("findBySlugAndStatus: returns empty for wrong status (draft)")
    void findBySlugAndStatus_wrongStatus() {
        savePost("Draft Post", "draft-post", PostStatus.DRAFT, category);

        Optional<Post> result = postRepository.findBySlugAndStatus("draft-post", PostStatus.PUBLISHED);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdWithAuthorAndCategory: loads relations")
    void findByIdWithAuthorAndCategory_loadsRelations() {
        Post post = savePost("My Post", "my-post", PostStatus.DRAFT, category);
        em.clear();

        Optional<Post> result = postRepository.findByIdWithAuthorAndCategory(post.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getAuthor()).isNotNull();
    }

    @Test
    @DisplayName("existsBySlug: returns true for existing slug")
    void existsBySlug_true() {
        savePost("Slug Post", "slug-post", PostStatus.DRAFT, null);

        assertThat(postRepository.existsBySlug("slug-post")).isTrue();
        assertThat(postRepository.existsBySlug("other-slug")).isFalse();
    }

    @Test
    @DisplayName("existsByCategory: true when posts reference category")
    void existsByCategory_true() {
        savePost("Cat Post", "cat-post", PostStatus.DRAFT, category);

        assertThat(postRepository.existsByCategory(category)).isTrue();
    }

    @Test
    @DisplayName("existsByCategory: false when no posts reference category")
    void existsByCategory_false() {
        Category emptyCategory = em.persistAndFlush(Category.builder()
                .name("Empty")
                .slug("empty")
                .build());

        assertThat(postRepository.existsByCategory(emptyCategory)).isFalse();
    }

    @Test
    @DisplayName("Specification: filters by status=PUBLISHED")
    void specification_statusFilter() {
        savePost("Published", "pub", PostStatus.PUBLISHED, category);
        savePost("Draft", "draft", PostStatus.DRAFT, category);

        Specification<Post> spec = (root, query, cb) -> cb.equal(root.get("status"), PostStatus.PUBLISHED);
        Page<Post> page = postRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getSlug()).isEqualTo("pub");
    }

    @Test
    @DisplayName("Specification: title search is case-insensitive")
    void specification_titleSearch() {
        savePost("Spring Tutorial", "spring-tutorial", PostStatus.PUBLISHED, category);
        savePost("Vue Guide", "vue-guide", PostStatus.PUBLISHED, category);

        Specification<Post> spec = (root, query, cb) -> {
            String pattern = "%" + "spring" + "%";
            return cb.like(cb.lower(root.get("title")), pattern);
        };
        Page<Post> page = postRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Spring Tutorial");
    }

    @Test
    @DisplayName("Unique slug constraint: duplicate slug throws")
    void duplicateSlug_throws() {
        savePost("First", "unique-slug", PostStatus.DRAFT, null);

        Post dup = Post.builder()
                .title("Second")
                .slug("unique-slug")
                .contentMarkdown("Content")
                .status(PostStatus.DRAFT)
                .author(author)
                .build();

        assertThatThrownBy(() -> em.persistAndFlush(dup))
                .isInstanceOf(Exception.class);
    }
}
