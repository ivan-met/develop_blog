# Plan: Blog Post Management Feature

> Companion to `docs/plans/user-management.md`. The REST API is the contract between the two agents.

## Context

User Management (auth, JWT, RBAC) is fully built and shipped (PR #1). The next feature is the
platform's reason to exist: **Blog Post Management** — letting developers create, edit, delete,
draft/publish, and preview technical posts, organized by technology **category**. This plan builds
the Post + Category domain end-to-end (backend REST API + Vue UI), reusing every convention already
established by the User Management feature.

The post-related RBAC rules were already **confirmed and recorded** in `docs/plans/user-management.md`
and are honored here verbatim:

| Actor | Can do |
|---|---|
| **Anonymous** | Read **published** posts only (public read). |
| **USER** | Create posts; edit/delete/publish **their own** posts. |
| **ADMIN** | **Cannot author** posts; can edit / unpublish / delete **any** post, and manages categories. |

Work is split between the two committed subagents; the REST API is the contract:
- **`spring-backend-developer`** → all backend work under `backend/devblog/devblog/`.
- **`vue-frontend-developer`** → all frontend work under `frontend/`.

## Decisions (confirmed with user)

| Topic | Decision |
|---|---|
| Content format | **Markdown** is the source of truth, stored raw (`content_markdown` TEXT). No server-side HTML. |
| Rendering | Rendered to HTML **client-side** and **sanitized** (markdown-it + DOMPurify). "Preview" = live rendered pane. |
| Categorization | **Managed `Category` entity**, admin-curated. Each post belongs to **one** category (`@ManyToOne`). |
| Images | **URL embeds only** inside Markdown (`![alt](https://…)`). No upload endpoint / blob storage. |
| Authoring vs admin | Per the confirmed rule, **anyone holding `ROLE_ADMIN` is denied post creation** (admins moderate, not author). Surfaced explicitly — flag if undesired. |
| Slugs | Slug generated from title at **creation**, uniqueness-suffixed, **stable thereafter** (no broken links on edit). |
| Category on publish | Category optional for a DRAFT; **required to PUBLISH** (validated in service → 400). |
| Category delete | **Blocked (409)** while any post references it. |

---

## Shared API Contract (source of truth for both agents)

Base path `/api`. JSON. Stateless JWT (`Authorization: Bearer <accessToken>`), exactly as today.

**Categories — public read** (`/api/categories`, permitAll):
- `GET /api/categories` → `200` `CategoryResponse[]`
- `GET /api/categories/{slug}` → `200` `CategoryResponse`

**Categories — admin** (`/api/admin/categories`, `ROLE_ADMIN`):
- `POST /api/admin/categories` — `{name, description?}` → `201` `CategoryResponse`
- `PUT  /api/admin/categories/{id}` — `{name, description?}` → `200` `CategoryResponse`
- `DELETE /api/admin/categories/{id}` → `204` (`409` if posts reference it)

**Posts — public read** (permitAll):
- `GET /api/posts` — paginated **published** only; `?category={slug}&search=&page=&size=` → `200` `Page<PostSummaryResponse>`
- `GET /api/posts/{slug}` — published detail → `200` `PostResponse` (`404` if draft/absent)

**Posts — authoring** (authenticated):
- `GET  /api/posts/mine` — caller's own posts (all statuses); `?status=DRAFT|PUBLISHED&page=&size=` → `200` `Page<PostSummaryResponse>`
- `GET  /api/posts/mine/{id}` — caller's own post incl. draft content (for editing) → `200` `PostResponse` (owner or admin)
- `POST /api/posts` — `{title, contentMarkdown, excerpt?, categoryId?, status?}` → `201` `PostResponse` (**denied to ADMIN** → 403)
- `PUT  /api/posts/{id}` — `{title, contentMarkdown, excerpt?, categoryId?}` → `200` `PostResponse` (owner or admin)
- `PUT  /api/posts/{id}/status` — `{status: "DRAFT"|"PUBLISHED"}` → `200` `PostResponse` (owner or admin; first publish sets `publishedAt`)
- `DELETE /api/posts/{id}` → `204` (owner or admin)

**DTO shapes:**
- `CategoryResponse`: `{id, name, slug, description}`
- `PostSummaryResponse` (lists, no body): `{id, slug, title, excerpt, status, category: CategoryResponse|null, author: AuthorSummary, publishedAt, createdAt}`
- `PostResponse` (detail): summary fields **plus** `contentMarkdown`, `updatedAt`
- `AuthorSummary`: `{id, username, displayName}`
- Errors: existing global JSON shape `{timestamp, status, error, message, fieldErrors?}`. Ownership violations → **403**.

---

## Backend — delegated to `spring-backend-developer`

Root: `backend/devblog/devblog/`. Base package `met.ivan.devblog`. **No new dependencies, no new config** —
Spring Security 7, JWT, validation, JPA + `JpaSpecificationExecutor`, SQLite, H2-for-tests are all already wired.
Follow the existing layering exactly: `entity` / `repository` / `service` (interface) / `service/impl` /
`mapper` (`@Component`, manual) / `dto` / `controller`, with `@RestControllerAdvice` global handler.

### 1. Domain & persistence (`entity/`, `repository/`)
- **`Post`** entity: `id`, `title`, `slug` (unique, indexed), `contentMarkdown` (`@Column(columnDefinition="TEXT")`),
  `excerpt` (nullable, ≤300), `status` (`@Enumerated(STRING)` → new `PostStatus`), `author` (`@ManyToOne` → `User`, not null),
  `category` (`@ManyToOne` → `Category`, nullable), `publishedAt` (Instant, nullable), `createdAt`/`updatedAt`
  (`@CreationTimestamp`/`@UpdateTimestamp`), `@Version`. Mirror auditing/versioning from `entity/User.java`.
- **`Category`** entity: `id`, `name` (unique), `slug` (unique, indexed), `description` (nullable), timestamps.
- **`PostStatus`** enum: `DRAFT`, `PUBLISHED` (mirror `entity/RoleName.java`).
- **`PostRepository extends JpaRepository<Post,Long>, JpaSpecificationExecutor<Post>`**:
  `findBySlug` / `findBySlugAndStatus` with `JOIN FETCH author, category`; `findByIdWithAuthorAndCategory`;
  `existsBySlug`. Public list & "mine" list use Specifications (status / category-slug / title-search predicates) —
  same pattern as `AdminUserServiceImpl`'s spec search.
- **`CategoryRepository`**: `findBySlug`, `existsByName`, `existsBySlugAndIdNot`, and a `countByCategory`/`existsByCategory`
  (on `PostRepository`) used to block category deletion.

### 2. Slug utility
Small helper (e.g. `service/impl` private method or `util/Slugs`): lowercase, strip accents, non-alphanumeric → `-`,
collapse/trim dashes; ensure uniqueness via `existsBySlug` loop appending `-2`, `-3`, …. Generated at creation only.

### 3. Services (`service/` + `service/impl/`)
- **`CategoryService`**: `list()`, `getBySlug()`, and admin `create` / `update` (reject duplicate name/slug →
  `DuplicateResourceException`) / `delete` (throw new `ConflictException`/reuse pattern → **409** if referenced).
- **`PostService`** (`@Transactional`):
  - `create(authorUsername, req)` — **reject if author holds `ROLE_ADMIN`** (→ 403); resolve category if `categoryId`
    given; if `status=PUBLISHED` require a category and set `publishedAt`; generate slug.
  - `update(id, principal, req)` — load post, **assert owner-or-admin**, patch fields (slug unchanged).
  - `changeStatus(id, principal, req)` — owner-or-admin; on first `PUBLISHED` set `publishedAt`; publishing requires category.
  - `delete(id, principal)` — owner-or-admin.
  - `getPublishedBySlug(slug)` — `PUBLISHED` only else `ResourceNotFoundException`.
  - `listPublished(categorySlug, search, pageable)` — Specification, status=PUBLISHED.
  - `getOwn(id, principal)` / `listOwn(username, status, pageable)` — owner-or-admin; any status.
  - Ownership/role checks throw a new `ForbiddenOperationException` (extends `RuntimeException`).

### 4. Mappers (`mapper/`)
- `CategoryMapper.toResponse`. `PostMapper.toResponse` / `toSummary` (build nested `AuthorSummary` + `CategoryResponse`;
  summary omits `contentMarkdown`). Manual, mirroring `mapper/UserMapper.java`.

### 5. Controllers (`controller/`) & security
- `CategoryController` (`/api/categories`), `AdminCategoryController` (`/api/admin/categories`,
  `@PreAuthorize("hasRole('ADMIN')")`), `PostController` (`/api/posts`, incl. `/mine` routes). Thin, `@Valid`,
  DTO-only, `@AuthenticationPrincipal UserDetails`, `@PageableDefault`.
- **`SecurityConfig` matcher additions** (ordering matters — owner/write routes before the public GET wildcard):
  ```
  .requestMatchers("/api/posts/mine/**").authenticated()
  .requestMatchers(HttpMethod.POST,   "/api/posts").authenticated()
  .requestMatchers(HttpMethod.PUT,    "/api/posts/**").authenticated()
  .requestMatchers(HttpMethod.DELETE, "/api/posts/**").authenticated()
  .requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/categories/**").permitAll()
  // existing: /api/admin/** -> hasRole("ADMIN") already covers /api/admin/categories/**
  ```
- **`GlobalExceptionHandler`**: add handlers for `ForbiddenOperationException` → **403** and the category-in-use
  conflict → **409** (reuse `DuplicateResourceException` mapping or add `ConflictException`).

### 6. Seeding (optional, `config/DataInitializer.java`)
Idempotently seed a few starter categories (e.g. `Java`, `Spring`, `Vue`, `DevOps`) if none exist — keeps the
browse UI non-empty on a fresh DB. No post seeding.

### 7. Tests (mandatory — gate `./mvnw test`)
- **Unit (Mockito)**: `PostServiceImpl` — create-as-user, **create-as-admin denied**, owner vs non-owner vs admin on
  update/delete (403 matrix), publish sets `publishedAt` + requires category, slug uniqueness; `CategoryServiceImpl` —
  CRUD, duplicate name, **delete blocked when referenced**; slug util.
- **`@DataJpaTest`**: `PostRepository` spec queries (published filter, by-category, search, `JOIN FETCH`), slug uniqueness;
  `CategoryRepository`. Reuse `TestDataFactory` (extend it with `post(...)`/`category(...)` builders).
- **`@WebMvcTest` (+ security)**: anonymous reads published (200) but draft → 404; `POST /api/posts` anonymous → 401,
  USER → 201, ADMIN → 403; `AdminCategoryController` USER → 403 / ADMIN → 200 (use `@WithMockUser`).
- **`@SpringBootTest` integration** (`TestRestTemplate`, profile `test`): user creates draft → absent from public list,
  present in `/mine` → publish → fetchable by slug & listed publicly → second user **cannot** edit it (403) → admin
  **can** edit/unpublish/delete it → category delete blocked while referenced.

---

## Frontend — delegated to `vue-frontend-developer`

Root: `frontend/`. Reuse the existing stack: axios `http.ts` (interceptors/refresh), Pinia `auth` store, router
meta-guards, `useFormValidation`, and the `AppInput`/`AppButton`/`FormField`/`AlertMessage`/`NavBar` components +
the "Midnight Ink" Tailwind theme. **Mirror the `users.ts` pattern**: API modules + view-local state (no posts store).

### 1. Dependencies
Add `markdown-it` + `dompurify` (+ `highlight.js` for code blocks) and their `@types`. Markdown is rendered then
**sanitized with DOMPurify** before injecting via `v-html` — never inject unsanitized.

### 2. API layer & types (`src/api/`)
- Extend `src/api/types.ts`: `PostStatus`, `CategoryResponse`, `AuthorSummary`, `PostSummaryResponse`, `PostResponse`,
  `CreatePostRequest`, `UpdatePostRequest`, `UpdatePostStatusRequest`, `CreateCategoryRequest`/`UpdateCategoryRequest`
  (kept in sync with backend DTOs; reuse existing `Page<T>`).
- `src/api/posts.ts`: `listPublished`, `getBySlug`, `listMine`, `getMine`, `create`, `update`, `changeStatus`, `remove`.
- `src/api/categories.ts`: `list`, `getBySlug`, admin `create`/`update`/`remove`.

### 3. Components (`src/components/`)
- **`MarkdownEditor.vue`** — split textarea + live `MarkdownPreview` pane (the "rich text editor" + preview requirement).
- **`MarkdownPreview.vue`** — markdown-it → DOMPurify → `v-html`; highlight code blocks.
- **`PostCard.vue`** — summary card for lists; **`StatusBadge.vue`** (Draft/Published); **`CategorySelect.vue`**.

### 4. Views (`src/views/`)
- **`PostsListView`** (public home `/`) — published feed, category filter, search, pagination.
- **`PostDetailView`** (`/posts/:slug`) — rendered post + author/category meta; owner/admin see Edit.
- **`PostEditorView`** (`/posts/new`, `/posts/:id/edit`) — title, `CategorySelect`, excerpt, `MarkdownEditor`,
  **Save draft** / **Publish** / **Unpublish**; loads via `getMine` when editing; validation via `useFormValidation`.
- **`MyPostsView`** (`/me/posts`) — caller's posts (status filter) with edit/publish/delete actions.
- **`admin/CategoriesView`** (`/admin/categories`) — admin category CRUD table.

### 5. Routing & nav (`src/router/index.ts`, `NavBar.vue`)
Add `/` (public), `/posts/:slug` (public), `/posts/new` + `/posts/:id/edit` + `/me/posts`
(`meta.requiresAuth`), `/admin/categories` (`meta.requiresAdmin`) — reuse existing guard logic. NavBar: "Browse",
plus "New Post" / "My Posts" when authenticated, "Categories" for admins. Hide "New Post" for admins (matches the
no-author rule). Responsive; use the `frontend-design` skill for the editor/list layouts.

### 6. Tests (mandatory — gate `npm run test` + `npm run build`)
Vitest + Vue Test Utils: `posts`/`categories` API modules; **`MarkdownPreview` sanitization** (a `<script>`/`onerror`
payload is stripped); `PostEditorView` validation + correct create/update/publish payloads; `MyPostsView` actions;
route guards for `/posts/new`, `/posts/:id/edit`, `/admin/categories`; `CategoriesView` admin CRUD with mocked API.

---

## Execution order (delegation)

1. **Backend agent first** — build Post/Category API + tests on a branch; report the **finalized contract** (any deviations).
2. **Frontend agent** — implement against the confirmed contract on a branch; can build components/design in parallel.
3. Each agent runs its test gate before finishing; commit on a branch (not `main`).

## Verification (end-to-end)

- **Backend**: `cd backend/devblog/devblog && ./mvnw test` → green. `./mvnw spring-boot:run`; with curl/HTTP client
  (reusing seeded `user` / `admin`): login as **user** → `POST /api/posts` (draft) → confirm absent from `GET /api/posts`
  but present in `GET /api/posts/mine` → `PUT /api/posts/{id}/status {PUBLISHED}` → `GET /api/posts/{slug}` returns it →
  login as **admin** → `POST /api/posts` returns **403**, but admin can `PUT`/`DELETE` the user's post → `DELETE`
  a referenced category returns **409**.
- **Frontend**: `cd frontend && npm run test` (green) + `npm run build` (clean). `npm run dev`: as a user, write a
  Markdown post, watch the live preview, save a draft, see it in My Posts, publish it, view it on the public list/detail;
  as admin, manage categories and edit/unpublish someone's post; confirm a draft 404s for anonymous and "New Post" is
  hidden for admins.
- **Integration**: run both together; verify public browse works **without** a token, authoring requires login, and the
  401→refresh path still holds while editing.