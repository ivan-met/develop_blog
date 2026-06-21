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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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

    private Post savePostFull(String title, String slug, PostStatus status, Category cat,
                              String content, Set<String> tags, long viewCount, Instant publishedAt) {
        Post p = Post.builder()
                .title(title)
                .slug(slug)
                .contentMarkdown(content)
                .status(status)
                .author(author)
                .category(cat)
                .tags(new LinkedHashSet<>(tags))
                .viewCount(viewCount)
                .publishedAt(publishedAt)
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

    // --- Tags and viewCount tests ---

    @Test
    @DisplayName("Specification: search matches on contentMarkdown (not just title)")
    void specification_contentSearch() {
        Instant now = Instant.now();
        savePostFull("About Streams", "about-streams", PostStatus.PUBLISHED, category,
                "Java streams are lazy evaluated functional pipelines", Set.of("java"), 0L, now);
        savePostFull("Vue Components", "vue-components", PostStatus.PUBLISHED, category,
                "Vue single file components provide a clean structure", Set.of("vue"), 0L, now);
        em.clear();

        Specification<Post> spec = (root, query, cb) -> {
            String pattern = "%lazy evaluated%";
            return cb.like(cb.lower(root.get("contentMarkdown")), pattern);
        };
        Page<Post> page = postRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getSlug()).isEqualTo("about-streams");
    }

    @Test
    @DisplayName("Specification: search matches on tag value")
    void specification_tagSearch() {
        Instant now = Instant.now();
        savePostFull("Spring Security Post", "spring-sec-post", PostStatus.PUBLISHED, category,
                "Spring Security content here", Set.of("spring-security", "jwt"), 0L, now);
        savePostFull("Vue Guide", "vue-guide-tag", PostStatus.PUBLISHED, category,
                "Vue guide content", Set.of("vue3", "typescript"), 0L, now);
        em.clear();

        Specification<Post> spec = (root, query, cb) -> {
            String pattern = "%jwt%";
            Join<Post, String> tagJoin = root.join("tags", JoinType.LEFT);
            query.distinct(true);
            return cb.like(cb.lower(tagJoin), pattern);
        };
        Page<Post> page = postRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getSlug()).isEqualTo("spring-sec-post");
    }

    @Test
    @DisplayName("incrementViewCount: atomically increments count by 1")
    void incrementViewCount_raisesValue() {
        Instant now = Instant.now();
        Post post = savePostFull("View Test Post", "view-test-post", PostStatus.PUBLISHED, category,
                "Content", Set.of("test"), 10L, now);
        em.clear();

        postRepository.incrementViewCount(post.getId());
        em.clear();

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updated.getViewCount()).isEqualTo(11L);
    }

    @Test
    @DisplayName("Sort: POPULAR order returns highest viewCount first")
    void sort_popular_highestViewCountFirst() {
        Instant now = Instant.now();
        savePostFull("Low Views", "low-views", PostStatus.PUBLISHED, category,
                "Content", Set.of(), 10L, now.minus(3, ChronoUnit.DAYS));
        savePostFull("High Views", "high-views", PostStatus.PUBLISHED, category,
                "Content", Set.of(), 500L, now.minus(1, ChronoUnit.DAYS));
        savePostFull("Mid Views", "mid-views", PostStatus.PUBLISHED, category,
                "Content", Set.of(), 100L, now.minus(2, ChronoUnit.DAYS));
        em.clear();

        Page<Post> page = postRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("status"), PostStatus.PUBLISHED),
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("publishedAt"))));

        List<Post> posts = page.getContent();
        assertThat(posts.get(0).getSlug()).isEqualTo("high-views");
        assertThat(posts.get(1).getSlug()).isEqualTo("mid-views");
        assertThat(posts.get(2).getSlug()).isEqualTo("low-views");
    }

    @Test
    @DisplayName("Sort: LATEST order returns most recently published first")
    void sort_latest_mostRecentFirst() {
        Instant now = Instant.now();
        savePostFull("Oldest", "oldest-post", PostStatus.PUBLISHED, category,
                "Content", Set.of(), 100L, now.minus(10, ChronoUnit.DAYS));
        savePostFull("Newest", "newest-post", PostStatus.PUBLISHED, category,
                "Content", Set.of(), 5L, now.minus(1, ChronoUnit.DAYS));
        savePostFull("Middle", "middle-post", PostStatus.PUBLISHED, category,
                "Content", Set.of(), 50L, now.minus(5, ChronoUnit.DAYS));
        em.clear();

        Page<Post> page = postRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("status"), PostStatus.PUBLISHED),
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("publishedAt"))));

        List<Post> posts = page.getContent();
        assertThat(posts.get(0).getSlug()).isEqualTo("newest-post");
        assertThat(posts.get(1).getSlug()).isEqualTo("middle-post");
        assertThat(posts.get(2).getSlug()).isEqualTo("oldest-post");
    }
}
