package met.ivan.devblog.repository;

import met.ivan.devblog.entity.Role;
import met.ivan.devblog.entity.RoleName;
import met.ivan.devblog.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole = roleRepository.save(Role.builder().name(RoleName.USER).build());
        adminRole = roleRepository.save(Role.builder().name(RoleName.ADMIN).build());
    }

    private User saveUser(String username, String email, Set<Role> roles) {
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash("hash")
                .active(true)
                .roles(new HashSet<>(roles))
                .build();
        return em.persistAndFlush(user);
    }

    @Test
    @DisplayName("findByUsernameWithRoles: returns user with roles loaded")
    void findByUsernameWithRoles_loadsRoles() {
        saveUser("alice", "alice@test.com", Set.of(userRole));

        Optional<User> result = userRepository.findByUsernameWithRoles("alice");

        assertThat(result).isPresent();
        assertThat(result.get().getRoles()).hasSize(1);
        assertThat(result.get().getRoles().iterator().next().getName()).isEqualTo(RoleName.USER);
    }

    @Test
    @DisplayName("findByUsernameOrEmailWithRoles: finds by username")
    void findByUsernameOrEmailWithRoles_byUsername() {
        saveUser("bob", "bob@test.com", Set.of(userRole));

        Optional<User> result = userRepository.findByUsernameOrEmailWithRoles("bob");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("bob");
    }

    @Test
    @DisplayName("findByUsernameOrEmailWithRoles: finds by email")
    void findByUsernameOrEmailWithRoles_byEmail() {
        saveUser("charlie", "charlie@test.com", Set.of(userRole));

        Optional<User> result = userRepository.findByUsernameOrEmailWithRoles("charlie@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("charlie@test.com");
    }

    @Test
    @DisplayName("existsByUsername: returns true for existing username")
    void existsByUsername_existingUser() {
        saveUser("dave", "dave@test.com", Set.of(userRole));

        assertThat(userRepository.existsByUsername("dave")).isTrue();
        assertThat(userRepository.existsByUsername("unknown")).isFalse();
    }

    @Test
    @DisplayName("existsByEmail: returns true for existing email")
    void existsByEmail_existingEmail() {
        saveUser("eve", "eve@test.com", Set.of(userRole));

        assertThat(userRepository.existsByEmail("eve@test.com")).isTrue();
        assertThat(userRepository.existsByEmail("other@test.com")).isFalse();
    }

    @Test
    @DisplayName("existsByEmailAndIdNot: returns true when email taken by different user")
    void existsByEmailAndIdNot() {
        User user1 = saveUser("user1", "shared@test.com", Set.of(userRole));
        User user2 = saveUser("user2", "unique@test.com", Set.of(userRole));

        assertThat(userRepository.existsByEmailAndIdNot("shared@test.com", user2.getId())).isTrue();
        assertThat(userRepository.existsByEmailAndIdNot("shared@test.com", user1.getId())).isFalse();
    }

    @Test
    @DisplayName("findByIdWithRoles: loads user with roles")
    void findByIdWithRoles_loadsRoles() {
        User user = saveUser("frank", "frank@test.com", Set.of(userRole, adminRole));

        Optional<User> result = userRepository.findByIdWithRoles(user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getRoles()).hasSize(2);
    }

    @Test
    @DisplayName("unique constraint violation on duplicate username")
    void duplicateUsername_throwsException() {
        saveUser("grace", "grace@test.com", Set.of(userRole));

        User duplicate = User.builder()
                .username("grace")
                .email("grace2@test.com")
                .passwordHash("hash")
                .active(true)
                .roles(new HashSet<>())
                .build();

        assertThatThrownBy(() -> em.persistAndFlush(duplicate))
                .isInstanceOf(Exception.class);
    }
}
