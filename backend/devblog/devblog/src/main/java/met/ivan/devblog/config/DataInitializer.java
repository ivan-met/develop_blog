package met.ivan.devblog.config;

import met.ivan.devblog.entity.Category;
import met.ivan.devblog.entity.Role;
import met.ivan.devblog.entity.RoleName;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.repository.CategoryRepository;
import met.ivan.devblog.repository.RoleRepository;
import met.ivan.devblog.repository.UserRepository;
import met.ivan.devblog.util.Slugs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public DataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            PasswordEncoder passwordEncoder,
            AppProperties appProperties) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Ensure roles exist
        Role adminRole = ensureRole(RoleName.ADMIN);
        Role userRole = ensureRole(RoleName.USER);

        // Seed default admin (holds both ADMIN and USER roles)
        AppProperties.SeedUser adminProps = appProperties.getSeed().getAdmin();
        if (!userRepository.existsByUsername(adminProps.getUsername())) {
            User admin = User.builder()
                    .username(adminProps.getUsername())
                    .email(adminProps.getEmail())
                    .passwordHash(passwordEncoder.encode(adminProps.getPassword()))
                    .active(true)
                    .roles(new HashSet<>(Set.of(adminRole, userRole)))
                    .build();
            userRepository.save(admin);
            log.info("Seeded default admin user: {}", adminProps.getUsername());
        }

        // Seed default user (holds USER role only)
        AppProperties.SeedUser userProps = appProperties.getSeed().getUser();
        if (!userRepository.existsByUsername(userProps.getUsername())) {
            User defaultUser = User.builder()
                    .username(userProps.getUsername())
                    .email(userProps.getEmail())
                    .passwordHash(passwordEncoder.encode(userProps.getPassword()))
                    .active(true)
                    .roles(new HashSet<>(Set.of(userRole)))
                    .build();
            userRepository.save(defaultUser);
            log.info("Seeded default user: {}", userProps.getUsername());
        }

        // Seed starter categories (idempotent — only if none exist)
        if (categoryRepository.count() == 0) {
            seedCategory("Java", "Articles about the Java programming language and JVM ecosystem");
            seedCategory("Spring", "Spring Framework, Spring Boot, and related projects");
            seedCategory("Vue", "Vue.js 3 frontend development and ecosystem");
            seedCategory("DevOps", "CI/CD, containers, cloud infrastructure, and deployment practices");
        }
    }

    private void seedCategory(String name, String description) {
        String slug = Slugs.toSlug(name);
        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .description(description)
                .build();
        categoryRepository.save(category);
        log.info("Seeded category: {}", name);
    }

    private Role ensureRole(RoleName name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = Role.builder().name(name).build();
            Role saved = roleRepository.save(role);
            log.info("Created role: {}", name);
            return saved;
        });
    }
}
