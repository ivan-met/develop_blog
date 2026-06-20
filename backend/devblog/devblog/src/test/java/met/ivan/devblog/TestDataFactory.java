package met.ivan.devblog;

import met.ivan.devblog.entity.Category;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.entity.Role;
import met.ivan.devblog.entity.RoleName;
import met.ivan.devblog.entity.User;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class TestDataFactory {

    public static Role userRole() {
        return Role.builder().id(1L).name(RoleName.USER).build();
    }

    public static Role adminRole() {
        return Role.builder().id(2L).name(RoleName.ADMIN).build();
    }

    public static User userWithRole(Role role) {
        return User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$04$hashed")
                .displayName("Test User")
                .bio("Test bio")
                .avatarUrl(null)
                .active(true)
                .roles(new HashSet<>(Set.of(role)))
                .createdAt(Instant.now())
                .build();
    }

    public static User adminUser(Role adminRole, Role userRole) {
        return User.builder()
                .id(2L)
                .username("admin")
                .email("admin@example.com")
                .passwordHash("$2a$04$hashed")
                .displayName("Admin User")
                .active(true)
                .roles(new HashSet<>(Set.of(adminRole, userRole)))
                .createdAt(Instant.now())
                .build();
    }

    public static Category category() {
        return Category.builder()
                .id(1L)
                .name("Java")
                .slug("java")
                .description("Java programming")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static Category category(Long id, String name, String slug) {
        return Category.builder()
                .id(id)
                .name(name)
                .slug(slug)
                .description(name + " description")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static Post draftPost(User author, Category category) {
        return Post.builder()
                .id(10L)
                .title("Test Post")
                .slug("test-post")
                .contentMarkdown("# Hello")
                .excerpt("A test post")
                .status(PostStatus.DRAFT)
                .author(author)
                .category(category)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static Post publishedPost(User author, Category category) {
        return Post.builder()
                .id(11L)
                .title("Published Post")
                .slug("published-post")
                .contentMarkdown("# Published")
                .excerpt("A published post")
                .status(PostStatus.PUBLISHED)
                .author(author)
                .category(category)
                .publishedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
