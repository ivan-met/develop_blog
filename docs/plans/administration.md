# Administration Feature Plan

## Context

The CLAUDE.md "Administration" capability calls for four admin powers: **manage categories**,
**moderate comments**, **manage users and content**, and **view platform statistics**.

Exploration showed **two of the four already ship end-to-end**:

- **Manage categories** — `AdminCategoryController` (POST/PUT/DELETE `/api/admin/categories`) +
  `frontend/src/views/admin/CategoriesView.vue` (full CRUD).
- **Manage users** — `AdminUserController` (list/search, roles, status) +
  `frontend/src/views/admin/UsersView.vue` (role + active toggles).

The gaps this plan fills:

1. **Moderate comments** — no admin/global comment view exists (only per-post listing). Admins
   *can* already hard-delete any comment via `DELETE /api/comments/{id}`.
2. **Manage content** — no admin listing of all posts across authors/statuses. Admins *can*
   already change status / delete a post by id.
3. **View platform statistics** — nothing exists.
4. **Admin hub** — the existing admin views are reachable only by typing URLs; no unified landing
   page or nav grouping.

Decisions confirmed with the user:
- Comment moderation = **global list + reuse existing hard delete** (no schema change).
- Content management = **list all posts + reuse existing unpublish (changeStatus) + delete** (no schema change).
- Statistics = **headline totals + top lists** (single aggregate endpoint).
- Add a unified **`/admin` dashboard hub** + admin nav.

**Outcome:** a complete Administration area — dashboard hub, comment moderation, content
management, and statistics — built almost entirely by adding *read/aggregate* admin endpoints
and views, reusing existing mutation endpoints. No database migrations, no entity changes.

---

## Architecture Notes / Reuse

- Admin authorization is already enforced two ways: `SecurityConfig` maps `/api/admin/**` →
  `hasRole("ADMIN")`, and admin controllers carry class-level `@PreAuthorize("hasRole('ADMIN')")`.
  **All new admin endpoints live under `/api/admin/**`** and follow this same pattern — no
  security wiring changes needed.
- Reuse existing DTOs: `PostSummaryResponse`, `CommentResponse`, `CategoryResponse`,
  `UserResponse`, `AuthorSummary`.
- Reuse existing mappers: `CommentMapper.toResponse`, `PostMapper`, `CategoryMapper`.
- Reuse existing mutation endpoints from the frontend (no new backend write paths):
  - Delete comment → `DELETE /api/comments/{id}` (`commentsApi.remove`).
  - Change post status / delete post → `PUT /api/posts/{id}/status`, `DELETE /api/posts/{id}`
    (`postsApi.changeStatus`, `postsApi.remove`) — both already allow admin.
- Frontend reuse: `isAdmin` guard in `router/index.ts`, `AppButton`, `FormField`, `AppInput`,
  `AlertMessage`, `StatusBadge`, paginated-table + search patterns from `UsersView.vue`, the
  `Page<T>` type, and the Axios 401-refresh client.
- The work splits cleanly along the agent contract: **backend agent** owns all of section A,
  **frontend agent** owns all of section B. The REST shapes in section A are the contract.

---

## A. Backend (spring-backend-developer agent)

Base package `met.ivan.devblog`. Maven project root: `backend/devblog/devblog/`.

### A1. Comment moderation — global admin comment listing

New read endpoint; deletion reuses the existing `DELETE /api/comments/{id}`.

- **`AdminCommentController`** (`controller/`), class-level `@PreAuthorize("hasRole('ADMIN')")`:
  - `GET /api/admin/comments` → `Page<AdminCommentResponse>`, params: optional `search`
    (matches comment content, author username, or post title), `@PageableDefault(size = 20)`,
    default sort `createdAt DESC`.
- **`AdminCommentResponse`** DTO (`dto/`): `id`, `content`, `author` (reuse `AuthorSummary`),
  `postSlug`, `postTitle`, `createdAt`. (Richer than `CommentResponse` because it must identify
  which post each comment belongs to.)
- **`AdminCommentService` / `AdminCommentServiceImpl`** (`service/`, `service/impl/`):
  `Page<AdminCommentResponse> list(String search, Pageable)`.
- **`CommentRepository`**: add a paginated query that JOIN FETCHes `c.author`, `c.post`,
  `p.author`, with an optional case-insensitive search across content / author username / post
  title and an explicit `countQuery` (mirror the existing `findByPostIdWithAuthor` pattern to
  avoid N+1 and Hibernate count-with-fetch issues). A `@Query` with
  `(:search IS NULL OR LOWER(...) LIKE ...)` is sufficient; no Specification needed.

### A2. Content management — global admin post listing

New read endpoint; status-change and delete reuse `PUT /api/posts/{id}/status` and
`DELETE /api/posts/{id}` (already admin-capable in `PostServiceImpl`).

- **`AdminPostController`** (`controller/`), class-level `@PreAuthorize("hasRole('ADMIN')")`:
  - `GET /api/admin/posts` → `Page<PostSummaryResponse>`, params: optional `status`
    (`DRAFT|PUBLISHED`), optional `search` (title/tags), optional `categorySlug`,
    `@PageableDefault(size = 20)`, default sort `createdAt DESC`. Unlike the public
    `GET /api/posts`, this returns posts of **all authors and all statuses**.
- **`AdminPostService` / `AdminPostServiceImpl`**: build a `Specification<Post>` (PostRepository
  already extends `JpaSpecificationExecutor`) combining optional status / category / search
  filters, map via existing `PostMapper` to `PostSummaryResponse`.

### A3. Platform statistics

- **`AdminStatsController`** (`controller/`), class-level `@PreAuthorize("hasRole('ADMIN')")`:
  - `GET /api/admin/stats` → `PlatformStatsResponse`.
- **`PlatformStatsResponse`** DTO with nested records:
  - `totals`: `users`, `activeUsers`, `posts`, `publishedPosts`, `draftPosts`, `comments`,
    `categories`, `likes`, `bookmarks`.
  - `topPostsByViews`: `List<PostSummaryResponse>` (top 5).
  - `topPostsByLikes`: `List<TopPostResponse>` (slug, title, author, likeCount) — top 5.
  - `recentUsers`: `List<RecentUserResponse>` (username, displayName, createdAt) — latest 5.
- **`AdminStatsService` / `AdminStatsServiceImpl`**: aggregate via repositories. Use existing
  `JpaRepository.count()` where possible; add the few count/aggregate queries needed:
  - `UserRepository.countByActiveTrue()`.
  - `PostRepository.countByStatus(PostStatus)`.
  - `PostLikeRepository` group-by-post count for top-by-likes (e.g. a `@Query` returning post +
    like count, `Pageable` limited to 5) — check the existing like repository for a reusable
    count method first.
  - Top posts by views: `PostRepository` derived query
    `findTop5ByStatusOrderByViewCountDesc(PostStatus.PUBLISHED)`.
  - Recent users: `UserRepository` derived query `findTop5ByOrderByCreatedAtDesc()`.

### A4. Backend tests (mandatory)

Mirror existing patterns (`TestDataFactory`, `@WebMvcTest` slice tests, `@ExtendWith(MockitoExtension.class)`
service tests, `@SpringBootTest` integration tests). For each new controller/service add:

- **Service unit tests** (`AdminCommentServiceImplTest`, `AdminPostServiceImplTest`,
  `AdminStatsServiceImplTest`): mock repositories + mappers; verify filtering, search, mapping,
  aggregation math, empty/edge cases.
- **Controller slice tests** (`@WebMvcTest` + `@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})`):
  - Authorization matrix per endpoint: `@WithMockUser(roles = "ADMIN")` → 200; `roles = "USER"`
    → 403; anonymous → 401.
  - Param handling (search/status/pagination), JSON shape via `jsonPath`.
- **Repository tests** for the new `CommentRepository` search query and any new
  `PostRepository`/like aggregate queries (follow `CategoryRepositoryTest`/`CommentRepositoryTest`).
- **Integration test** (`AdminModerationIntegrationTest` or extend an existing admin IT):
  end-to-end as a seeded admin — list comments, delete one (reuse existing endpoint), list all
  posts incl. drafts, unpublish a post, fetch `/api/admin/stats` and assert totals match seeded
  data; assert a USER gets 403 on each new endpoint.
- Run `mvnw.cmd test` (JDK 21 — set `JAVA_HOME` to corretto-21 per project memory) and ensure green.

---

## B. Frontend (vue-frontend-developer agent)

Root: `frontend/`. Follow the GitHub-dark Tailwind conventions and the `// admin.x` monospace
header style already used in `UsersView.vue` / `CategoriesView.vue`.

### B1. API layer & types (`src/api/`)

- **`api/types.ts`**: add `AdminCommentResponse`, `PlatformStatsResponse` (+ nested
  `StatsTotals`, `TopPostResponse`, `RecentUserResponse`) matching the backend DTOs.
- **`api/comments.ts`**: add `listAll(params: { search?; page?; size? })` → `GET /admin/comments`.
  (Keep existing `remove(id)` for deletion.)
- **`api/posts.ts`**: add `listAdmin(params: { status?; search?; categorySlug?; page?; size? })`
  → `GET /admin/posts`. (Reuse existing `changeStatus`, `remove`.)
- **`api/stats.ts`** (new): `getStats()` → `GET /admin/stats`.

### B2. Views (`src/views/admin/`)

- **`DashboardView.vue`** (`/admin`): fetch `/admin/stats`; render headline count cards
  (users/active, posts/published/draft, comments, categories, likes, bookmarks), the two
  top-post lists, and recent signups; plus navigation cards linking to Users, Categories,
  Comments, Content. Reuse `AlertMessage` + loading state.
- **`CommentsView.vue`** (`/admin/comments`): paginated table (author, content excerpt, post
  title→link, createdAt, delete action) with debounced search — model on `UsersView.vue`. Delete
  via `commentsApi.remove(id)` behind a `window.confirm` (matches `MyPostsView` pattern);
  per-row loading state; refresh list after delete.
- **`ContentView.vue`** (`/admin/posts`): paginated table of all posts (title→link, author,
  category, `StatusBadge`, viewCount, likeCount, createdAt) with search + status filter
  (All/Draft/Published) + optional category filter. Actions: Unpublish/Publish via
  `postsApi.changeStatus`, Delete via `postsApi.remove` (confirm dialog). Per-row loading.

### B3. Routing & nav

- **`router/index.ts`**: add four `requiresAuth + requiresAdmin` routes — `/admin` (dashboard),
  `/admin/comments`, `/admin/posts` — alongside the existing `/admin/users`, `/admin/categories`.
  The existing global guard already handles `requiresAdmin` (loads `loadMe()`, checks `isAdmin`).
- **`NavBar.vue`**: the admin entry (already role-gated) should point to `/admin` and/or expose an
  admin dropdown/section linking the five admin views. Keep it visible only when `auth.isAdmin`.

### B4. Frontend tests (mandatory)

Mirror existing Vitest patterns (mock `@/api/*` modules, `createTestRouter`, fresh Pinia,
`flushPromises`, `vi.spyOn(window, 'confirm')`):

- **View tests**: `DashboardView.test.ts` (renders totals/top lists from mocked stats, loading +
  error states), `CommentsView.test.ts` (lists, search debounce, delete confirm + refresh),
  `ContentView.test.ts` (lists, status filter, unpublish/publish + delete confirm).
- **Router guard test**: extend `test/router/guards.test.ts` to cover the new admin routes
  (USER/anon redirected away, ADMIN allowed).
- **API module test** (if `api/*` modules are unit-tested in the repo): cover `stats.getStats`,
  `comments.listAll`, `posts.listAdmin` request shapes.
- Run `npm run lint` (type-check) and `npm test`; ensure green.

---

## C. Documentation

After both halves land, update:
- **`CLAUDE.md`** "Current State" — note the Administration area (dashboard, comment moderation,
  content management, statistics) and the new `/api/admin/*` endpoints.
- **`README.md`** REST API reference — document `GET /api/admin/comments`, `GET /api/admin/posts`,
  `GET /api/admin/stats`.

---

## Verification (end-to-end)

1. **Backend**: from `backend/devblog/devblog/` run `mvnw.cmd test` → all green (new unit, slice,
   repository, integration tests included).
2. **Frontend**: from `frontend/` run `npm run lint` and `npm test` → all green.
3. **Manual smoke** (`mvnw.cmd spring-boot:run` + `npm run dev`):
   - Log in as the seeded **admin**; confirm an Admin entry in the nav → `/admin` dashboard shows
     non-zero seeded totals, top-post lists, and recent users.
   - `/admin/comments`: search, then delete a seeded comment; list refreshes and the public post's
     comment count drops.
   - `/admin/posts`: see drafts + published from all authors; unpublish a published post (verify it
     disappears from the public home list) and re-publish it; delete a throwaway post.
   - `/admin/users` and `/admin/categories` still work (regression check).
   - Log in as the seeded **non-admin user**: no admin nav; navigating to `/admin`, `/admin/comments`,
     `/admin/posts` redirects home; hitting the `/api/admin/*` endpoints directly returns 403.

---

## Out of Scope (per user decisions)

- No comment `status`/flagging/report system or soft-delete (chose global list + hard delete).
- No `featured`/`pinned` post flag (chose list + unpublish/delete).
- No time-series charts / admin audit log / user suspension (chose totals + top lists).
- No database migrations or entity changes anywhere in this feature.
