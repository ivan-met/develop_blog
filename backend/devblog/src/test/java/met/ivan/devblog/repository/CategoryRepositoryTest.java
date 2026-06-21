package met.ivan.devblog.repository;

import met.ivan.devblog.entity.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CategoryRepository")
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category saveCategory(String name, String slug) {
        Category c = Category.builder().name(name).slug(slug).description(name + " desc").build();
        return em.persistAndFlush(c);
    }

    @Test
    @DisplayName("findBySlug: returns category for known slug")
    void findBySlug_found() {
        saveCategory("Java", "java");

        Optional<Category> result = categoryRepository.findBySlug("java");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Java");
    }

    @Test
    @DisplayName("findBySlug: returns empty for unknown slug")
    void findBySlug_notFound() {
        Optional<Category> result = categoryRepository.findBySlug("unknown");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByName: true for existing name")
    void existsByName_true() {
        saveCategory("Spring", "spring");
        assertThat(categoryRepository.existsByName("Spring")).isTrue();
        assertThat(categoryRepository.existsByName("Other")).isFalse();
    }

    @Test
    @DisplayName("existsBySlug: true for existing slug")
    void existsBySlug_true() {
        saveCategory("Vue", "vue");
        assertThat(categoryRepository.existsBySlug("vue")).isTrue();
        assertThat(categoryRepository.existsBySlug("react")).isFalse();
    }

    @Test
    @DisplayName("existsByNameAndIdNot: detects name clash with other category")
    void existsByNameAndIdNot() {
        Category c1 = saveCategory("DevOps", "devops");
        Category c2 = saveCategory("Docker", "docker");

        assertThat(categoryRepository.existsByNameAndIdNot("DevOps", c2.getId())).isTrue();
        assertThat(categoryRepository.existsByNameAndIdNot("DevOps", c1.getId())).isFalse();
    }

    @Test
    @DisplayName("existsBySlugAndIdNot: detects slug clash with other category")
    void existsBySlugAndIdNot() {
        Category c1 = saveCategory("Go Lang", "go-lang");
        Category c2 = saveCategory("Rust", "rust");

        assertThat(categoryRepository.existsBySlugAndIdNot("go-lang", c2.getId())).isTrue();
        assertThat(categoryRepository.existsBySlugAndIdNot("go-lang", c1.getId())).isFalse();
    }

    @Test
    @DisplayName("unique constraint: duplicate name throws")
    void duplicateName_throws() {
        saveCategory("Java", "java");

        Category dup = Category.builder().name("Java").slug("java-2").build();

        assertThatThrownBy(() -> em.persistAndFlush(dup))
                .isInstanceOf(Exception.class);
    }
}
