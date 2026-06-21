package met.ivan.devblog.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Slugs utility")
class SlugsTest {

    @ParameterizedTest(name = "''{0}'' -> ''{1}''")
    @CsvSource({
            "Hello World,           hello-world",
            "Spring Boot,           spring-boot",
            "Java & JVM,            java-jvm",
            "  leading/trailing  ,  leading-trailing",
            "Café au lait,          cafe-au-lait",
            "Multiple   Spaces,     multiple-spaces",
            "UPPERCASE,             uppercase",
            "123 Numbers,           123-numbers",
            "---dashes---,          dashes"
    })
    @DisplayName("toSlug converts titles to valid slugs")
    void toSlug_variousInputs(String input, String expected) {
        assertThat(Slugs.toSlug(input.trim())).isEqualTo(expected.trim());
    }

    @Test
    @DisplayName("toSlug: null input returns 'post'")
    void toSlug_null_returnsPost() {
        assertThat(Slugs.toSlug(null)).isEqualTo("post");
    }

    @Test
    @DisplayName("toSlug: blank input returns 'post'")
    void toSlug_blank_returnsPost() {
        assertThat(Slugs.toSlug("   ")).isEqualTo("post");
    }

    @Test
    @DisplayName("uniqueSlug: returns base when no collision")
    void uniqueSlug_noCollision() {
        String slug = Slugs.uniqueSlug("My Title", s -> false);
        assertThat(slug).isEqualTo("my-title");
    }

    @Test
    @DisplayName("uniqueSlug: appends -2 on first collision")
    void uniqueSlug_oneCollision() {
        Set<String> taken = Set.of("my-title");
        String slug = Slugs.uniqueSlug("My Title", taken::contains);
        assertThat(slug).isEqualTo("my-title-2");
    }

    @Test
    @DisplayName("uniqueSlug: appends -3 on two collisions")
    void uniqueSlug_twoCollisions() {
        Set<String> taken = Set.of("my-title", "my-title-2");
        String slug = Slugs.uniqueSlug("My Title", taken::contains);
        assertThat(slug).isEqualTo("my-title-3");
    }
}
