# Plan: User Management Feature

## Context

The Developer Blog Platform needs its foundational **User Management** feature: registration, authentication, profile management, and role-based access control. Today the backend is a bare Spring Boot 4.1.0 scaffold (package `met.ivan.devblog`) with JPA + Web MVC + SQLite + Lombok only — no Security, JWT, QueryDSL, or validation — and the `frontend/` directory is empty. This feature establishes the auth backbone every later feature (posts, comments, moderation) depends on, and defines the JWT contract between the two modules.

Work is split between two committed subagents:
- **`spring-backend-developer`** → all backend work under `backend/devblog/devblog/`.
- **`vue-frontend-developer`** → all frontend work under `frontend/` (scaffolds the Vue app).

The contract between them is the REST API + JWT, defined in this document.

## Decisions (confirmed with user)

| Topic | Decision |
|---|---|
| Roles | **Two roles: `ADMIN`, `USER`** — many-to-many, admin-managed. A user may hold both. |
| USER permissions | Create posts; edit own posts. |
| ADMIN permissions | **Cannot** create posts; can edit/moderate all content and manage users. |
| Anonymous | Read posts only (public read). |
| Seeding | On startup seed a default **admin** (holds ADMIN **and** USER) and a default **user** (USER), credentials from config/env. |
| Auth | Local register/login only — **no external/social login**. |
| Tokens | **Access + refresh** JWTs; `/api/auth/refresh` endpoint; refresh tokens persisted for rotation/logout. |
| Frontend token storage | **localStorage**, sent as `Authorization: Bearer` via an axios interceptor. |
| Tests | **Mandatory** on both sides — backend and frontend. |

> Note: post create/edit endpoints belong to a **future Posts feature**. This plan builds the auth + RBAC foundation and encodes the permission rules above so they're ready to enforce. The `spring-boot-engineer` skill assumes Spring Boot 3.x/Security 6 — the backend agent must adapt patterns to **Spring Boot 4 / Spring Security 7** (Jakarta namespace, current `SecurityFilterChain` lambda DSL).

---

## Shared API Contract (source of truth for both agents)

Base path `/api`. JSON. Stateless JWT (`Authorization: Bearer <accessToken>`).

**Auth** (`/api/auth/**`, public):
- `POST /register` — `{username, email, password}` → creates a USER → `201` `{accessToken, refreshToken, user}`
- `POST /login` — `{usernameOrEmail, password}` → `200` `{accessToken, refreshToken, user}`
- `POST /refresh` — `{refreshToken}` → `200` `{accessToken, refreshToken}` (rotates refresh token)
- `POST /logout` — `{refreshToken}` → `204` (invalidates refresh token)

**Profile** (`/api/users/me`, authenticated):
- `GET /me` → `200` `UserResponse`
- `PUT /me` — `{displayName, bio, avatarUrl, email}` → `200` `UserResponse`
- `PUT /me/password` — `{currentPassword, newPassword}` → `204`

**Admin user management** (`/api/admin/users/**`, ROLE_ADMIN only):
- `GET /` — paginated (`?page&size&search`) → `200` `Page<UserResponse>`
- `GET /{id}` → `200` `UserResponse`
- `PUT /{id}/roles` — `{roles: ["ADMIN","USER"]}` → `200` `UserResponse`
- `PUT /{id}/status` — `{active: boolean}` → `200` `UserResponse`

**DTO shapes:**
- `UserResponse`: `{id, username, email, displayName, bio, avatarUrl, roles: string[], active, createdAt}`
- Authorities map roles → `ROLE_ADMIN`, `ROLE_USER`.
- Errors: consistent JSON `{timestamp, status, error, message, fieldErrors?}` from a global handler.

---

## Backend — delegated to `spring-backend-developer`

Root: `backend/devblog/devblog/`. Base package `met.ivan.devblog`.

**Package layout convention:** `controller` (REST controllers + `@RestControllerAdvice`), `entity` (JPA entities), `dto` (request/response DTOs), `repository` (Spring Data repositories), `service` (service **interfaces**), `service/impl` (service implementations). Security and config classes live in `security` and `config`.

### 1. Dependencies (`pom.xml`)
Add: `spring-boot-starter-security`, `spring-boot-starter-validation`, and `io.jsonwebtoken:jjwt-api/jjwt-impl/jjwt-jackson` (0.12.x). QueryDSL is **not required** for this feature — use Spring Data derived queries + an optional `JpaSpecificationExecutor` for the admin user search; defer QueryDSL until a feature needs it.

### 2. Configuration
- `application.yaml`: SQLite datasource (`jdbc:sqlite:devblog.db`, `org.hibernate.community.dialect.SQLiteDialect` if needed), `jpa.hibernate.ddl-auto: update`, and `jwt.secret` / `jwt.access-expiration` (~15m) / `jwt.refresh-expiration` (~7d) read from env. Seed creds under `app.seed.*`. **No secrets hardcoded.**
- `application-test.yaml`: H2 in-memory, `ddl-auto: create-drop`, profile `test`, fixed/short BCrypt strength for speed.

### 3. Domain & persistence (`entity/`, `repository/`)
- `User` entity: `id`, `username` (unique), `email` (unique), `passwordHash`, `displayName`, `bio`, `avatarUrl`, `active`, auditing (`createdAt`/`updatedAt`), `@Version`; `@ManyToMany` `roles` via `user_roles` join table.
- `Role` entity: `id`, `name` (enum-backed: `ADMIN`, `USER`).
- `RefreshToken` entity: `id`, `token`, `user`, `expiresAt`, `revoked` — for rotation/logout.
- Repositories: `UserRepository` (`findByUsername`, `findByEmail`, `findByUsernameOrEmail` with `JOIN FETCH roles`, `existsBy...`, `JpaSpecificationExecutor`), `RoleRepository`, `RefreshTokenRepository`.

### 4. Security (`security/`, `config/`)
- `SecurityConfig`: `@EnableWebSecurity @EnableMethodSecurity`, stateless, `BCryptPasswordEncoder`, public `/api/auth/**`, `ROLE_ADMIN` on `/api/admin/**`, authenticated on `/api/users/**`; CORS for the Vite dev origin; CSRF disabled (stateless API); custom `AuthenticationEntryPoint` returning JSON 401.
- `JwtService` (jjwt): generate/parse/validate access & refresh tokens, extract username + roles.
- `JwtAuthenticationFilter extends OncePerRequestFilter`: validate bearer token, populate `SecurityContext`.
- `CustomUserDetailsService`: load user (with roles) by username/email.

### 5. Services & controllers (`service/` + `service/impl/`, `controller/`, `dto/`)
- Each service is an **interface** in `service/` with its implementation in `service/impl/` (e.g. `AuthenticationService` → `service/impl/AuthenticationServiceImpl`).
- `AuthenticationService` (transactional): register (assign USER, reject duplicate username/email), login, refresh (rotate), logout (revoke).
- `UserService`: profile read/update, change password (verify current).
- `AdminUserService`: paginated/search list, get, set roles, set active status.
- Controllers (in `controller/`): `AuthController`, `UserController`, `AdminUserController` — thin, validated (`@Valid`), DTO-mapped (never expose entities). Method-level `@PreAuthorize` where useful.
- `GlobalExceptionHandler` (`@RestControllerAdvice`, in `controller/`): validation errors, auth failures, duplicate-resource, not-found → consistent JSON.

### 6. Seeding (`config/DataInitializer.java`)
`CommandLineRunner`/`ApplicationRunner`: ensure `ADMIN` + `USER` roles exist; create the default admin (both roles) and default user (USER) from `app.seed.*` config **only if absent**. Idempotent.

### 7. Tests (mandatory)
- **Unit (Mockito)**: `AuthenticationService` (register/login/refresh/duplicate), `JwtService` (round-trip, expiry, tampering), `UserService` (password change), `AdminUserService` (role/status changes).
- **`@DataJpaTest`**: `UserRepository` queries incl. `JOIN FETCH roles`, uniqueness constraints.
- **`@WebMvcTest`** (+ security): `AuthController` register/login validation; `UserController` with `@WithMockUser`; `AdminUserController` — assert 403 for USER, 200 for ADMIN.
- **`@SpringBootTest` integration** (`TestRestTemplate`, profile `test`): full flow register → login → call protected `/me` → refresh → admin-only endpoint authorization. Plus seeding test (default users exist).
- Gate: `./mvnw test` must pass before the backend work is considered done.

---

## Frontend — delegated to `vue-frontend-developer`

Root: `frontend/` (currently empty — **scaffold first**).

### 1. Scaffold
`npm create vite@latest` → Vue 3 + TypeScript. Add **Tailwind CSS**, **Vue Router**, **Pinia**, **axios**, **Vitest** + **@vue/test-utils**. Strict TS. `<script setup>` Composition API only.

### 2. API layer (`src/api/`)
- `http.ts`: axios instance with `baseURL` (proxied to backend), request interceptor attaching `Bearer` from the auth store, response interceptor that on `401` tries `/auth/refresh` once then logs out on failure.
- `auth.ts`, `users.ts`: typed functions mirroring the API contract; TS interfaces for `UserResponse`, `AuthResponse`, request bodies (kept in sync with backend DTOs).

### 3. State (`src/stores/auth.ts`, Pinia)
Holds access/refresh tokens (persisted to **localStorage**) + current user; getters `isAuthenticated`, `isAdmin`, `isUser`; actions `register`, `login`, `logout`, `refresh`, `loadMe`. Rehydrate from localStorage on app start.

### 4. Routing & guards (`src/router/index.ts`)
Routes: `/login`, `/register`, `/profile` (auth), `/admin/users` (admin). Global guard: redirect unauthenticated users away from protected routes; `requireAdmin` guard checks `isAdmin`. Public/anonymous can reach read-only/home routes.

### 5. Views & components (`src/views/`, `src/components/`)
- `LoginView`, `RegisterView` (validated forms, error display).
- `ProfileView`: view/edit profile (displayName, bio, avatarUrl, email) + change-password form.
- `admin/UsersView`: paginated user table with search, role toggles (ADMIN/USER), activate/deactivate — calls admin endpoints.
- `NavBar`: reflects auth state, shows admin link only for admins, logout. Responsive/mobile-friendly (Tailwind); intentional design via the `frontend-design` skill.

### 6. Tests (mandatory)
Vitest + Vue Test Utils: auth store (login/logout/refresh/role getters with mocked API), `LoginView`/`RegisterView` validation & submit, route-guard logic (redirect rules), and the http interceptor's 401→refresh path. Gate: `npm run test` + `npm run build` must pass.

---

## Testing Strategy

Testing is **mandatory** and a completion gate on both sides. The strategy is a layered pyramid: many fast unit tests, a focused set of integration tests that exercise real wiring, and a thin slice of end-to-end checks (see Verification). Target ≥ 80% coverage on services, security, and the auth/RBAC paths.

### Backend (JUnit 5)

**Unit tests** — fast, isolated, no Spring context; collaborators mocked with Mockito (`@ExtendWith(MockitoExtension.class)`, `@InjectMocks`). Cover business logic and edge cases:
- `AuthenticationServiceImpl` — register (happy path, duplicate username, duplicate email, default USER role assigned), login (valid, bad password, unknown user, inactive user), refresh (valid, expired, revoked, rotation issues a new token), logout (revokes token).
- `JwtService` — access/refresh generation + parse round-trip, role claims extraction, expired token rejected, tampered/invalid-signature token rejected.
- `UserServiceImpl` — profile update, change password (correct current password succeeds, wrong current fails), email-uniqueness on update.
- `AdminUserServiceImpl` — assign/revoke roles (incl. admin holding both), activate/deactivate, paginated search filtering.
- Assert with AAA structure + `@DisplayName`; verify mock interactions; cover both success and exception branches.

**Integration tests** — real Spring context + real DB wiring (H2 in-memory, profile `test`):
- **Persistence slice** (`@DataJpaTest`, `@AutoConfigureTestDatabase(replace = NONE)`): `UserRepository` derived/`JOIN FETCH roles` queries, unique-constraint violations on username/email, `RefreshTokenRepository` lookup/expiry, `RoleRepository`. Use `TestEntityManager` to seed.
- **Web slice** (`@WebMvcTest` + imported `SecurityConfig`, `MockMvc`): request validation (400 + field errors), auth endpoints, and **authorization matrix** — anonymous → 401 on protected routes, USER → 403 on `/api/admin/**` (use `@WithMockUser(roles="USER")`), ADMIN → 200 (`roles="ADMIN"`). Service layer mocked here.
- **Full-stack flow** (`@SpringBootTest(webEnvironment = RANDOM_PORT)`, `TestRestTemplate`): end-to-end through real filters/security — register → login (capture tokens) → call `/api/users/me` with bearer → update profile → `/api/auth/refresh` (old refresh rejected after rotation) → `/api/auth/logout` (token no longer valid). Plus an RBAC test (USER blocked from admin, seeded admin allowed) and a **seeding test** asserting the default admin (both roles) and default user exist on startup.
- Shared helpers: `application-test.yaml` (H2, `create-drop`, low BCrypt strength, fixed `Clock` for deterministic token expiry) and a `TestDataFactory` for building users/roles/requests.

**Gate:** `./mvnw test` green before backend work is considered done.

### Frontend (Vitest + @vue/test-utils)

**Unit tests** — components, store, and pure logic with the API/network mocked:
- `auth` Pinia store: `login`/`register` persist tokens to localStorage + set user; `logout` clears them; `refresh` swaps tokens; getters `isAuthenticated`/`isAdmin`/`isUser` derive correctly from roles; rehydration from localStorage on init.
- `LoginView` / `RegisterView`: client-side validation, submit calls the store action, server error surfaces in the UI.
- `ProfileView`: edit + change-password forms call the right services with correct payloads.
- `http.ts` interceptors: request attaches `Authorization: Bearer`; response `401` triggers a single `/auth/refresh` then retries, and logs out when refresh fails.
- Route guards: unauthenticated → redirect to `/login`; non-admin → blocked from `/admin/users`; admin allowed.

**Integration tests** — multi-unit interaction without a live backend: mount a view within a real router + Pinia store and a mocked HTTP layer (e.g. MSW or axios mock), then assert flows like "fill login form → store updates → guard now permits `/profile`" and "admin user table loads, role toggle issues the expected PUT and reflects the new state."

**Gate:** `npm run test` (green) + `npm run build` (clean) before frontend work is considered done.

### End-to-end (manual, see Verification)
Run backend + frontend together and walk the real browser flow (register, login, profile edit, admin user management, 401→refresh on access-token expiry). Automated E2E (Playwright/Cypress) is **out of scope** for this feature but a candidate for a later hardening pass.

---

## Execution order (delegation)

1. **Backend agent first** builds the API + seeding + tests and reports the **finalized API contract** (any deviations from this doc).
2. **Frontend agent** scaffolds and implements against that contract. It can start scaffolding + design in parallel, but wires real calls once the backend endpoints are confirmed.
3. Each agent commits its work on a branch (not `main`) and runs its test gate before finishing.

## Verification (end-to-end)

- **Backend**: `cd backend/devblog/devblog && ./mvnw test` → all green. Run `./mvnw spring-boot:run`; with `curl`/HTTP client: register → login (get tokens) → `GET /api/users/me` with bearer → `PUT /api/users/me` → `/api/auth/refresh` → confirm `GET /api/admin/users` returns **403** for a USER token and **200** for the seeded admin. Confirm seeded admin + user exist on a fresh DB.
- **Frontend**: `cd frontend && npm run test` (green) and `npm run build` (clean). `npm run dev`: register a new account, log in, edit profile + change password, log out; log in as seeded admin and confirm the `/admin/users` page loads and role/status changes persist; confirm a normal user is redirected away from `/admin/users`.
- **Integration**: run backend + frontend together; verify the full browser flow uses real JWTs and the 401→refresh path works (let an access token expire).