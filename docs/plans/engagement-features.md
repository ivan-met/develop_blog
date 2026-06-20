# Engagement Features — Implementation Plan

## Context

The Developer Blog Platform currently supports authoring, browsing, and admin management of
posts, but offers no way for readers to engage with content or with each other. This plan adds
three engagement features so the platform becomes a knowledge-sharing hub rather than a one-way
publishing tool:

1. **Comments** — authenticated users discuss published posts.
2. **Likes & Bookmarks** — likes are a public popularity signal; bookmarks are a private
   save-for-later list.
3. **Author profiles** — a public page showing an author's info and their published posts.

Decisions confirmed with the user:
- Comments are a **flat list** (no threading).
- Implement **both** likes (public counter) and bookmarks (private list).
- Author profile is addressed **by username** (`/authors/:username`); never exposes email/roles.
- **`popular` sort stays on `viewCount`**; `likeCount` is shown but does not change sort logic.

The contract between the two halves is the REST API (see CLAUDE.md "Working With Agents").
Backend work is delegated to the **spring-backend-developer** agent; frontend work to the
**vue-frontend-developer** agent. The backend is the source of truth — build and merge it first
so the frontend integrates against a stable API.

All paths below are relative to the package root
`backend/devblog/devblog/src/main/java/met/ivan/devblog/` or `frontend/src/`.

---

## API Contract (source of truth — build first)

### Comments
- `GET  /api/posts/{slug}/comments?page&size` → `Page<CommentResponse>` (public; newest first)
- `POST /api/posts/{slug}/comments` → `CommentResponse` (auth; body `{ content }`, 201)
- `DELETE /api/comments/{id}` → 204 (auth; comment author, **post author**, or admin)

`CommentResponse`: `{ id, content, author: AuthorSummary, createdAt, canDelete }`
(`canDelete` computed per requesting principal so the UI can show the delete button).

### Likes (public counter + toggle)
- `POST   /api/posts/{slug}/like` → `{ likeCount, liked }` (auth; idempotent)
- `DELETE /api/posts/{slug}/like` → `{ likeCount, liked }` (auth; idempotent)

### Bookmarks (private)
- `POST   /api/posts/{slug}/bookmark` → `{ bookmarked }` (auth)
- `DELETE /api/posts/{slug}/bookmark` → `{ bookmarked }` (auth)
- `GET    /api/users/me/bookmarks?page&size` → `Page<PostSummaryResponse>` (auth)

### Author profile
- `GET /api/authors/{username}` → `AuthorProfileResponse` (public)
- `GET /api/authors/{username}/posts?page&size` → `Page<PostSummaryResponse>` (public; PUBLISHED only)

`AuthorProfileResponse`: `{ username, displayName, bio, avatarUrl, createdAt, postCount }`
(deliberately omits email, roles, active, id).

### Enrichment of existing post payloads
`PostResponse` (single post) gains: `likeCount`, `liked`, `bookmarked`.
`PostSummaryResponse` (list cards) gains: `likeCount` only (cheap aggregate; no per-user flags in lists).

---

## Backend (delegate to spring-backend-developer)

Follow existing conventions exactly: feature/layer packages, DTOs at the boundary, Lombok
builders, `@AuthenticationPrincipal UserDetails` in controllers, username-based user resolution
in services, `Slugs`-style utilities, `@Transactional` boundaries, and `GlobalExceptionHandler`
domain exceptions (`ResourceNotFoundException`, `ForbiddenOperationException`,
`DuplicateResourceException`).

### Entities (`entity/`)
- **`Comment`** — `id`, `content` (TEXT, not null, max ~2000 via DTO validation), `@ManyToOne(LAZY) Post post`,
  `@ManyToOne(LAZY) User author`, `@CreationTimestamp createdAt`. Index on `post_id`. Mirror the
  id strategy and auditing style of `Post`.
- **`PostLike`** — `id`, `@ManyToOne(LAZY) User user`, `@ManyToOne(LAZY) Post post`, `createdAt`,
  with `@UniqueConstraint(columnNames = {"user_id","post_id"})`.
- **`PostBookmark`** — same shape as `PostLike` (separate table, same unique constraint).

### Repositories (`repository/`)
- **`CommentRepository`** — `Page<Comment> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable)`
  with `@Query ... JOIN FETCH c.author` to avoid N+1 (mirror `PostRepository` fetch style).
- **`PostLikeRepository`** — `boolean existsByUserIdAndPostId(...)`,
  `long countByPostId(Long postId)`, `void deleteByUserIdAndPostId(...)`,
  and `@Query` for batch like counts by post-id set (to enrich list responses without N+1).
- **`PostBookmarkRepository`** — `existsByUserIdAndPostId`, `deleteByUserIdAndPostId`,
  and a query returning the user's bookmarked posts page (JOIN FETCH author/category).
- Reuse existing `PostRepository.countByAuthorUsername(...)` for `AuthorProfileResponse.postCount`.

### DTOs (`dto/`) & Mappers (`mapper/`)
- New: `CommentResponse`, `CreateCommentRequest` (`@NotBlank`, `@Size`), `LikeResponse`,
  `BookmarkResponse`, `AuthorProfileResponse`.
- New `CommentMapper`; extend `PostMapper.toResponse` / `toSummary` to accept like/bookmark data.
  Keep mappers pure — pass counts/flags in as params (mapper has no repo access today).
- Extend `UserMapper` (or add a method) for `AuthorProfileResponse`.

### Services (`service/`)
- **`CommentService` / `CommentServiceImpl`** — `list(slug, pageable)`, `create(slug, username, req)`
  (reject if post not PUBLISHED → `ResourceNotFoundException`), `delete(id, principal)` with
  owner/post-author/admin check using the existing admin-authority pattern
  (`ROLE_ADMIN` in `principal.getAuthorities()`).
- **`EngagementService` / impl** — `like`/`unlike`/`bookmark`/`removeBookmark` (idempotent via
  `existsBy...` guards), `listBookmarks(username, pageable)`. Likes/unlikes return fresh count.
- **`AuthorService` / impl** (or extend `UserService`) — `getPublicProfile(username)`,
  `getPublishedPosts(username, pageable)` reusing the published `Specification` filtered by author.
- Update **`PostServiceImpl.getPublishedBySlug`** and `listPublished` to populate the new
  `PostResponse`/`PostSummaryResponse` fields. Resolve `liked`/`bookmarked` only when a principal
  is present (anonymous → `false`/omitted). For lists, do one batch like-count query keyed by the
  page's post ids — **do not** issue a query per row.

### Controllers (`controller/`)
- **`CommentController`** — `/api/posts/{slug}/comments` (GET, POST) and `/api/comments/{id}` (DELETE).
- **`EngagementController`** — `/api/posts/{slug}/like` (POST/DELETE),
  `/api/posts/{slug}/bookmark` (POST/DELETE), `/api/users/me/bookmarks` (GET).
- **`AuthorController`** — `/api/authors/{username}` and `/api/authors/{username}/posts` (GET).
- The public-read GET wildcard `/api/posts/**` already permits the comments GET; POST comment and
  like/bookmark fall through to `anyRequest().authenticated()`.

### Security (`config/SecurityConfig.java`)
- Add **one** rule so author endpoints are public:
  `.requestMatchers(HttpMethod.GET, "/api/authors/**").permitAll()`
  (place it alongside the existing public GET matchers, before `anyRequest().authenticated()`).
- `/api/users/me/bookmarks` is already covered by the `/api/users/**` authenticated rule.
- No change needed for comments/likes — confirmed against current `SecurityConfig` matchers.

### Seed data (`config/DataInitializer.java`)
- Optionally seed a few comments and likes on the existing starter posts (authored by the seed
  user) so the UI has content on first run. Keep idempotent (only when the respective table is empty),
  matching the existing post/category seeding guard pattern.

---

## Frontend (delegate to vue-frontend-developer)

Follow existing conventions: typed Axios modules in `api/`, `Page<T>` pagination type, inline-style
+ CSS-variable theming, `AppButton`/`AlertMessage`/`FormField` primitives, `useFormValidation`,
router meta guards, optimistic UI with rollback on error.

### API layer (`api/`)
- **`api/types.ts`** — add `CommentResponse`, `LikeResponse`, `BookmarkResponse`,
  `AuthorProfileResponse`; add `likeCount`/`liked`/`bookmarked` to `PostResponse` and `likeCount`
  to `PostSummaryResponse`.
- **`api/comments.ts`** — `list(slug, {page,size})`, `create(slug, {content})`, `remove(id)`.
- **`api/engagement.ts`** — `like(slug)`, `unlike(slug)`, `bookmark(slug)`, `removeBookmark(slug)`,
  `listMyBookmarks({page,size})`.
- **`api/authors.ts`** — `getProfile(username)`, `getPosts(username, {page,size})`.

### Components (`components/`)
- **`LikeButton.vue`** — heart toggle + count; optimistic update, reverts on error; disabled/redirects
  to `/login` when unauthenticated.
- **`BookmarkButton.vue`** — bookmark toggle; same auth handling.
- **`CommentList.vue`** + **`CommentForm.vue`** — list with pagination + relative dates; form uses
  `useFormValidation`; render delete button when `comment.canDelete`.
- **`AuthorByline.vue`** (small) — author avatar/name in post header/cards, now a `RouterLink` to
  `/authors/:username`.

### Views (`views/`)
- **`PostDetailView.vue`** — add like + bookmark buttons in the header; render `CommentList` +
  `CommentForm` below the markdown body; link author name to their profile.
- **`PostCard.vue`** — show `likeCount` (read-only) next to view count.
- **`AuthorProfileView.vue`** (new, public, route `/authors/:username`) — header (displayName, bio,
  avatar, join date, post count) + paginated grid of `PostCard`s reusing the `PostsListView` layout.
- **`BookmarksView.vue`** (new, protected, route `/me/bookmarks`) — paginated grid of bookmarked
  posts; add a NavBar link when authenticated.

### Router (`router/index.ts`)
- Add public `/authors/:username` → `AuthorProfileView` (`meta.public`).
- Add protected `/me/bookmarks` → `BookmarksView` (`meta.requiresAuth`).

---

## Test Strategy

Match the existing layered suite (`@ExtendWith(MockitoExtension.class)` unit, `@DataJpaTest`
repo slices, `@WebMvcTest` + `@WithMockUser` controller slices, `@SpringBootTest` +
`TestRestTemplate` integration). Reuse `TestDataFactory` and add builders for `Comment`,
`PostLike`, `PostBookmark`. Frontend mirrors `api/*.test.ts`, `stores/*.test.ts`,
`components/*.test.ts` with `vi.mock('@/api/http', ...)`.

### Backend
- **Unit** (`service/`): `CommentServiceImplTest` (create blocked on draft, delete authorization
  matrix: author vs post-author vs admin vs stranger), `EngagementServiceImplTest` (idempotent
  like/unlike, bookmark toggle, count correctness), `AuthorServiceImplTest` (published-only,
  unknown username → 404, postCount).
- **Repository** (`@DataJpaTest`): `CommentRepositoryTest` (ordering + JOIN FETCH author after
  `em.clear()`), `PostLikeRepositoryTest`/`PostBookmarkRepositoryTest` (unique-constraint violation
  on duplicate, count/exists queries).
- **Controller** (`@WebMvcTest`): for each new controller, the authorization matrix —
  anonymous read 200; anonymous write 401; authed write 201/200; forbidden delete 403; author GET
  public 200.
- **Integration** (`@SpringBootTest`): comment lifecycle (login → publish post → comment → list →
  delete); like idempotency + count reflected in `PostResponse`; bookmark then appears in
  `/api/users/me/bookmarks`; `/api/authors/{username}` hides email/roles and lists only published.

### Frontend (Vitest)
- API module tests: each `comments`/`engagement`/`authors` method calls the right URL with params.
- Component tests: `LikeButton` optimistic toggle + revert on rejected promise; `CommentForm`
  validation + emit; `CommentList` renders delete button only when `canDelete`.
- View test: `AuthorProfileView` renders profile + posts grid from mocked API.

---

## Verification (end-to-end)

1. **Backend** — from `backend/devblog/devblog/`:
   `mvn test` (set `JAVA_HOME` to corretto-21 first; see memory). All new + existing suites green.
2. **Frontend** — from `frontend/`: `npm test` and `npm run lint` (type-check). Green.
3. **Manual smoke** — run backend (`mvnw.cmd spring-boot:run`) + frontend (`npm run dev`):
   - Open a published post → like it (count increments, persists on reload) → bookmark it.
   - Post a comment; verify it appears; delete it (as author).
   - Visit `/me/bookmarks` → bookmarked post is listed.
   - Click the author name → `/authors/:username` shows bio + only their published posts.
   - Confirm an anonymous user can read comments/likes but is prompted to log in to interact.
4. **Contract check** — verify `/api/authors/{username}` JSON contains no `email`/`roles`/`id`.

---

## Sequencing

1. Backend agent: entities → repositories → DTOs/mappers → services → controllers → security rule
   → seed data → tests. Land and verify (`mvn test`) first — API is the contract.
2. Frontend agent: types → api modules → components → views → router → tests, integrating against
   the merged backend.
