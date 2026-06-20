package met.ivan.devblog.config;

import met.ivan.devblog.entity.Category;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.entity.Role;
import met.ivan.devblog.entity.RoleName;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.repository.CategoryRepository;
import met.ivan.devblog.repository.PostRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public DataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            PostRepository postRepository,
            PasswordEncoder passwordEncoder,
            AppProperties appProperties) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
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

        // Seed starter posts (idempotent — only if none exist)
        if (postRepository.count() == 0) {
            seedPosts(userProps.getUsername());
        }
    }

    private void seedPosts(String authorUsername) {
        User author = userRepository.findByUsername(authorUsername).orElse(null);
        if (author == null) {
            log.warn("Seed user '{}' not found; skipping post seeding", authorUsername);
            return;
        }

        Category java = categoryRepository.findBySlug("java").orElse(null);
        Category spring = categoryRepository.findBySlug("spring").orElse(null);
        Category vue = categoryRepository.findBySlug("vue").orElse(null);
        Category devops = categoryRepository.findBySlug("devops").orElse(null);

        Instant now = Instant.now();

        // Post 1 — Java, 30 days ago, high views
        savePost(author, java,
                "Getting Started with Java 21 Virtual Threads",
                "Discover how Project Loom's virtual threads revolutionize concurrency in Java 21.",
                """
                ## Introduction
                Java 21 introduced virtual threads as a standard feature through Project Loom.
                Unlike platform threads, virtual threads are lightweight and managed by the JVM scheduler.

                ## Creating Virtual Threads
                ```java
                Thread.ofVirtual().start(() -> System.out.println("Hello from virtual thread!"));
                ```

                ## Structured Concurrency
                Use `StructuredTaskScope` to manage groups of concurrent tasks with clean lifecycle control.
                Virtual threads make it practical to have millions of concurrent threads without exhausting OS resources.

                ## When to Use Them
                Virtual threads shine in I/O-bound workloads — HTTP calls, database queries, file reads.
                They are not a silver bullet for CPU-bound work.
                """,
                Set.of("java21", "virtual-threads", "concurrency", "project-loom"),
                now.minus(30, ChronoUnit.DAYS),
                4200L);

        // Post 2 — Spring, 25 days ago, very high views
        savePost(author, spring,
                "Spring Boot 3 Security with JWT — A Complete Guide",
                "Learn how to secure your Spring Boot REST API using stateless JWT authentication.",
                """
                ## Overview
                Spring Security 6 ships with Spring Boot 3 and brings a modernized, lambda-DSL configuration model.

                ## Adding Dependencies
                Include `spring-boot-starter-security` and `jjwt` (io.jsonwebtoken) in your `pom.xml`.

                ## SecurityFilterChain
                ```java
                @Bean
                public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                    return http
                        .csrf(AbstractHttpConfigurer::disable)
                        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
                        .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/auth/**").permitAll()
                            .anyRequest().authenticated())
                        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                        .build();
                }
                ```

                ## JWT Token Generation
                Sign tokens with a 256-bit secret key stored in environment variables, never in source code.
                """,
                Set.of("spring-security", "jwt", "spring-boot", "rest-api"),
                now.minus(25, ChronoUnit.DAYS),
                6800L);

        // Post 3 — Vue, 22 days ago, moderate views
        savePost(author, vue,
                "Vue 3 Composition API — Reusable Composables",
                "Master the Composition API by building reusable composables for common UI patterns.",
                """
                ## What Are Composables?
                Composables are functions that encapsulate and reuse stateful logic in Vue 3.
                They replace mixins with a cleaner, more explicit model.

                ## A Simple useFetch Composable
                ```typescript
                export function useFetch<T>(url: string) {
                  const data = ref<T | null>(null);
                  const loading = ref(true);
                  const error = ref<Error | null>(null);

                  onMounted(async () => {
                    try {
                      const res = await fetch(url);
                      data.value = await res.json();
                    } catch (e) {
                      error.value = e as Error;
                    } finally {
                      loading.value = false;
                    }
                  });

                  return { data, loading, error };
                }
                ```

                ## Benefits
                Composables are tree-shakeable, TypeScript-friendly, and easy to test in isolation.
                """,
                Set.of("vue3", "composition-api", "typescript", "composables"),
                now.minus(22, ChronoUnit.DAYS),
                2100L);

        // Post 4 — DevOps, 20 days ago, highest views
        savePost(author, devops,
                "Dockerizing a Spring Boot Application",
                "A step-by-step guide to containerizing your Spring Boot app with Docker and multi-stage builds.",
                """
                ## Why Docker?
                Containers give you reproducible environments from development all the way to production.

                ## Multi-Stage Dockerfile
                ```dockerfile
                FROM eclipse-temurin:21-jdk AS build
                WORKDIR /app
                COPY mvnw pom.xml ./
                COPY .mvn .mvn
                RUN ./mvnw dependency:go-offline -q
                COPY src src
                RUN ./mvnw package -DskipTests -q

                FROM eclipse-temurin:21-jre
                WORKDIR /app
                COPY --from=build /app/target/*.jar app.jar
                EXPOSE 8080
                ENTRYPOINT ["java", "-jar", "app.jar"]
                ```

                ## Running the Container
                ```bash
                docker build -t myapp:latest .
                docker run -p 8080:8080 -e JWT_SECRET=secret myapp:latest
                ```
                """,
                Set.of("docker", "spring-boot", "containers", "devops"),
                now.minus(20, ChronoUnit.DAYS),
                8500L);

        // Post 5 — Java, 18 days ago, low views
        savePost(author, java,
                "Understanding Java Stream API Best Practices",
                "Practical tips and common pitfalls when working with Java's Stream API.",
                """
                ## Streams Are Lazy
                Intermediate operations like `filter`, `map`, and `sorted` don't execute until a terminal operation is called.

                ## Avoid Stateful Lambdas
                ```java
                // BAD — mutating external state
                List<String> results = new ArrayList<>();
                stream.forEach(results::add);

                // GOOD — use collect
                List<String> results = stream.collect(Collectors.toList());
                ```

                ## Prefer Method References
                `stream.map(String::toLowerCase)` is cleaner than `stream.map(s -> s.toLowerCase())`.

                ## Parallel Streams
                Use `parallelStream()` only when the workload is CPU-bound and the collection is large enough to justify the overhead.
                """,
                Set.of("java", "streams", "functional-programming", "best-practices"),
                now.minus(18, ChronoUnit.DAYS),
                950L);

        // Post 6 — Spring, 15 days ago, high views
        savePost(author, spring,
                "Spring Data JPA — Advanced Querying with Specifications",
                "Go beyond findBy methods with JPA Specifications for dynamic, composable queries.",
                """
                ## The Problem with Derived Query Methods
                Method names get unwieldy when you need optional filters or OR conditions across multiple fields.

                ## JPA Specifications
                ```java
                public static Specification<Product> nameLike(String name) {
                    return (root, query, cb) ->
                        name == null ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
                }
                ```

                ## Combining Specifications
                ```java
                Specification<Product> spec = Specification
                    .where(nameLike(search))
                    .and(categoryEquals(categoryId));
                repository.findAll(spec, pageable);
                ```

                ## Element Collections
                When joining an `@ElementCollection`, call `query.distinct(true)` to avoid duplicate rows.
                """,
                Set.of("spring-data", "jpa", "specifications", "querydsl"),
                now.minus(15, ChronoUnit.DAYS),
                3400L);

        // Post 7 — Vue, 12 days ago, moderate views
        savePost(author, vue,
                "State Management with Pinia in Vue 3",
                "Replace Vuex with Pinia for a simpler, fully typed Vue 3 state management experience.",
                """
                ## Why Pinia?
                Pinia is the officially recommended state management library for Vue 3.
                It offers full TypeScript support, devtools integration, and a flat store model.

                ## Defining a Store
                ```typescript
                export const useAuthStore = defineStore('auth', () => {
                  const token = ref<string | null>(null);
                  const isAuthenticated = computed(() => token.value !== null);

                  function login(newToken: string) {
                    token.value = newToken;
                  }

                  function logout() {
                    token.value = null;
                  }

                  return { token, isAuthenticated, login, logout };
                });
                ```

                ## Persisting State
                Use the `pinia-plugin-persistedstate` plugin to persist stores to `localStorage` automatically.
                """,
                Set.of("vue3", "pinia", "state-management", "typescript"),
                now.minus(12, ChronoUnit.DAYS),
                1750L);

        // Post 8 — DevOps, 10 days ago, high views
        savePost(author, devops,
                "GitHub Actions CI/CD for Spring Boot",
                "Automate your build, test, and deployment pipeline with GitHub Actions.",
                """
                ## Workflow File
                ```yaml
                name: CI

                on:
                  push:
                    branches: [main]
                  pull_request:
                    branches: [main]

                jobs:
                  build:
                    runs-on: ubuntu-latest
                    steps:
                      - uses: actions/checkout@v4
                      - uses: actions/setup-java@v4
                        with:
                          java-version: '21'
                          distribution: 'temurin'
                      - name: Build and Test
                        run: ./mvnw verify
                ```

                ## Caching Dependencies
                Add a Maven cache step to speed up builds significantly:
                ```yaml
                - uses: actions/cache@v4
                  with:
                    path: ~/.m2
                    key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
                ```
                """,
                Set.of("github-actions", "ci-cd", "devops", "spring-boot"),
                now.minus(10, ChronoUnit.DAYS),
                5200L);

        // Post 9 — Java, 7 days ago, moderate views
        savePost(author, java,
                "Java Records — Immutable Data Classes",
                "Reduce boilerplate and model immutable data with Java records introduced in Java 16.",
                """
                ## What is a Record?
                A record is a special class that is implicitly final and provides canonical constructor,
                `equals`, `hashCode`, and `toString` out of the box.

                ```java
                public record UserDto(Long id, String username, String email) {}
                ```

                ## Compact Constructors
                Add validation in the compact constructor:
                ```java
                public record Email(String value) {
                    public Email {
                        if (!value.contains("@")) throw new IllegalArgumentException("Invalid email");
                    }
                }
                ```

                ## Records as DTOs
                Records are a natural fit for DTOs at the API boundary — they are immutable,
                serializable with Jackson, and require no Lombok.
                """,
                Set.of("java", "records", "immutability", "dto"),
                now.minus(7, ChronoUnit.DAYS),
                1200L);

        // Post 10 — Spring, 5 days ago, low views
        savePost(author, spring,
                "Spring Boot Actuator and Observability",
                "Expose health checks, metrics, and traces from your Spring Boot application.",
                """
                ## Adding Actuator
                Include `spring-boot-starter-actuator` in your `pom.xml`.
                By default, only `/actuator/health` and `/actuator/info` are exposed over HTTP.

                ## Exposing Endpoints
                ```yaml
                management:
                  endpoints:
                    web:
                      exposure:
                        include: health,info,metrics,prometheus
                ```

                ## Micrometer Metrics
                Spring Boot auto-configures Micrometer with support for Prometheus, Datadog, and many other backends.

                ## Distributed Tracing
                Add `spring-boot-starter-actuator` and `micrometer-tracing-bridge-otel` for OpenTelemetry traces.
                """,
                Set.of("spring-boot", "actuator", "observability", "metrics"),
                now.minus(5, ChronoUnit.DAYS),
                430L);

        // Post 11 — Vue, 3 days ago, very low views
        savePost(author, vue,
                "Building Accessible Forms with Vue 3",
                "Learn how to build keyboard-navigable, screen-reader-friendly forms in Vue 3.",
                """
                ## Why Accessibility Matters
                Approximately 15% of the world's population lives with some form of disability.
                Accessible forms ensure your application works for everyone.

                ## ARIA Attributes
                ```html
                <input
                  :id="fieldId"
                  :aria-describedby="errorId"
                  :aria-invalid="hasError"
                  v-model="value"
                />
                <p :id="errorId" role="alert" v-if="hasError">{{ errorMessage }}</p>
                ```

                ## Keyboard Navigation
                Ensure all interactive elements are reachable via Tab key and operable via Enter/Space.
                Avoid `div`-based click handlers — use semantic `<button>` elements.
                """,
                Set.of("vue3", "accessibility", "forms", "a11y"),
                now.minus(3, ChronoUnit.DAYS),
                180L);

        // Post 12 — DevOps, 1 day ago, very low views (newest)
        savePost(author, devops,
                "Kubernetes Basics for Java Developers",
                "A developer-friendly introduction to deploying Java applications on Kubernetes.",
                """
                ## Key Concepts
                - **Pod**: the smallest deployable unit; typically one container per pod.
                - **Deployment**: manages replica sets and rolling updates.
                - **Service**: exposes pods on a stable DNS name and IP.
                - **ConfigMap / Secret**: externalize configuration and credentials.

                ## Minimal Deployment YAML
                ```yaml
                apiVersion: apps/v1
                kind: Deployment
                metadata:
                  name: devblog
                spec:
                  replicas: 2
                  selector:
                    matchLabels:
                      app: devblog
                  template:
                    metadata:
                      labels:
                        app: devblog
                    spec:
                      containers:
                        - name: devblog
                          image: myregistry/devblog:latest
                          ports:
                            - containerPort: 8080
                          env:
                            - name: JWT_SECRET
                              valueFrom:
                                secretKeyRef:
                                  name: devblog-secrets
                                  key: jwt-secret
                ```

                ## Health Probes
                Configure `livenessProbe` and `readinessProbe` pointing at `/actuator/health` to let
                Kubernetes know when your pod is ready to serve traffic.
                """,
                Set.of("kubernetes", "devops", "java", "containers"),
                now.minus(1, ChronoUnit.DAYS),
                90L);

        log.info("Seeded 12 starter posts");
    }

    private void savePost(User author, Category category, String title, String excerpt,
                          String content, Set<String> tags, Instant publishedAt, long viewCount) {
        String slug = Slugs.uniqueSlug(title, postRepository::existsBySlug);
        Post post = Post.builder()
                .title(title)
                .slug(slug)
                .contentMarkdown(content)
                .excerpt(excerpt)
                .status(PostStatus.PUBLISHED)
                .author(author)
                .category(category)
                .tags(tags)
                .publishedAt(publishedAt)
                .viewCount(viewCount)
                .build();
        postRepository.save(post);
        log.debug("Seeded post: {}", title);
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
