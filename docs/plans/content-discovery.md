# Content Discovery — Implementation Plan

## Context

The platform already lets authors write and publish posts, and the public home page
(`PostsListView`) can browse by category and search by **title** with pagination. The
"Content Discovery" feature rounds this out into a real discovery experience:

- **Browse posts by category** — already works (`GET /api/posts?category=slug`); keep as-is.
- **Search posts by title, content, or tags** — today search hits **title only**, and
  **tags do not exist** anywhere in the model. Need to add tags and broaden search.
- **View latest and most popular articles** — there is **no sort control** and **no
  popularity signal** at all (no `viewCount`). Need both.
- **Filter content by technology** — the seeded categories (Java, Spring, Vue, DevOps)
  **are** the technologies, so this reuses the existing category filter.

There are also **no seed posts**, so the discovery UI looks empty on a fresh DB.

**Decisions (confirmed with user):**
1. Tags = lightweight `List<String>` on `Post` via JPA `@ElementCollection` (no Tag entity).
2. "Filter by technology" = existing **Category** filter (no new dimension).
3. "Most popular" = a `viewCount` field, **incremented on each public read** of a post.

Intended outcome: a public discovery page that can browse by technology/category, search
across title + content + tags, and toggle between **Latest** and **Most Popular**, backed
by realistic seed posts authored by the default `user`.

---

## Backend (delegate to `spring-backend-developer`)

Maven project root: `backend/devblog/devblog/`, package `met.ivan.devblog`.

### 1. Post entity — add tags + viewCount
`src/main/java/met/ivan/devblog/entity/Post.java`
- Add tags as an element collection:
  ```java
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "post_tags",
          joinColumns = @JoinColumn(name = "post_id"),
          indexes = @Index(name = "idx_post_tags_tag", columnList = "tag"))
  @Column(name = "tag", length = 50)
  @Builder.Default
  private Set<String> tags = new LinkedHashSet<>();
  ```
- Add popularity counter:
  ```java
  @Column(name = "view_count", nullable = false)
  @Builder.Default
  private Long viewCount = 0L;
  ```
  (Hibernate auto-DDL creates `post_tags` and the column on startup; SQLite dev DB and the
  in-memory test DB both regenerate schema.)

### 2. DTOs + mapper — surface tags & viewCount
- `dto/PostSummaryResponse.java` and `dto/PostResponse.java`: add `Set<String> tags` and
  `Long viewCount`.
- `dto/CreatePostRequest.java` and `dto/UpdatePostRequest.java`: add `Set<String> tags`
  (optional; bean-validate each entry, e.g. `@Size(max = 50)` on elements, cap list size).
- `mapper/PostMapper.java`: map `tags` and `viewCount` in both `toResponse` and `toSummary`;
  normalize tags on write (trim, lowercase, drop blanks, dedupe) — do this in
  `PostServiceImpl` create/update, not the mapper.

### 3. Search + sort in the service
`service/impl/PostServiceImpl.java` — `buildPublishedSpec(...)` and `listPublished(...)`:
- **Broaden search** (currently title-only at lines 206–209): OR across `title`,
  `contentMarkdown`, and the `tags` collection. Joining the element collection requires
  `query.distinct(true)`:
  ```java
  String pattern = "%" + search.toLowerCase() + "%";
  var titleLike   = cb.like(cb.lower(root.get("title")), pattern);
  var contentLike = cb.like(cb.lower(root.get("contentMarkdown")), pattern);
  var tagJoin     = root.join("tags", JoinType.LEFT);
  var tagLike     = cb.like(cb.lower(tagJoin), pattern);
  query.distinct(true);
  predicates.add(cb.or(titleLike, contentLike, tagLike));
  ```
- **Sort**: add a `sort` parameter (`String`/enum `LATEST | POPULAR`) to the service +
  controller. Translate to a `Sort` applied to the `Pageable` so the API never exposes raw
  column names:
  - `LATEST` (default) → `publishedAt DESC`
  - `POPULAR` → `viewCount DESC, publishedAt DESC`

  Build via `PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort)` rather
  than trusting client-supplied `Pageable.getSort()`.
- Tags also need normalization in `create()` / `update()` before save.

### 4. Increment viewCount on public read
`service/impl/PostServiceImpl.getPublishedBySlug(...)` (lines 133–139):
- Add an atomic increment that avoids the `@Version` optimistic-lock bump:
  `PostRepository`:
  ```java
  @Modifying
  @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
  void incrementViewCount(@Param("id") Long id);
  ```
- In the method: load the published post, call `incrementViewCount(post.getId())`, and
  reflect the new value in the response (set `viewCount + 1` on the returned DTO, or re-read).
  Remove `readOnly = true` from this method's `@Transactional` (it now writes).

### 5. Controller wiring
`controller/PostController.java` — `listPublished` (lines 31–37): add
`@RequestParam(required = false) String sort` and pass through. Keep `category` and `search`
params. Endpoint stays `GET /api/posts`, e.g.
`GET /api/posts?category=spring&search=jwt&sort=popular&page=0&size=12`.

### 6. Seed posts (authored by default `user`)
`config/DataInitializer.java`:
- Inject `PostRepository`.
- After categories are seeded, add `seedPosts(...)` guarded by `postRepository.count() == 0`.
- Look up the default user via `appProperties.getSeed().getUser().getUsername()` →
  `userRepository.findByUsername(...)`; author **all** seed posts with it.
- Create ~10–12 **PUBLISHED** posts spread across Java/Spring/Vue/DevOps, each with:
  `title`, generated `slug` (reuse `Slugs.uniqueSlug(title, postRepository::existsBySlug)`),
  short `excerpt`, a few paragraphs of markdown `contentMarkdown`, 2–4 `tags`, a `category`,
  a **staggered `publishedAt`** (e.g. now minus N days) so "Latest" ordering is meaningful,
  and a **varied `viewCount`** so "Most Popular" ordering is visibly different from latest.
- Idempotent and reuses the existing `Slugs` util and builder pattern already in the file.

### Backend tests (mirror existing style)
- `repository/PostRepositoryTest.java` (`@DataJpaTest`): persist posts with tags/content/
  varied viewCount; assert the search Specification matches on content and on a tag; assert
  `incrementViewCount` raises the value; assert sort orderings.
- `service/PostServiceImplTest.java` (Mockito): `listPublished` builds the right `Sort` for
  `latest` vs `popular`; tag normalization on create/update; `getPublishedBySlug` calls
  `incrementViewCount` and returns the bumped count.
- `controller/PostControllerTest.java` (`@WebMvcTest`): `?sort=popular` reaches the service;
  response JSON includes `tags` and `viewCount`.
- `integration/BlogPostManagementIntegrationTest.java` (`@SpringBootTest`): seeded posts are
  present and authored by `user`; `sort=popular` returns highest-viewed first; search by a
  content word and by a tag returns the expected post; a public GET bumps `viewCount`.
- Extend `TestDataFactory` builders to accept tags/viewCount.

---

## Frontend (delegate to `vue-frontend-developer`)

Project root: `frontend/`.

### 1. Types & API client
- `src/api/types.ts`: add `tags: string[]` and `viewCount: number` to `PostSummaryResponse`
  and `PostResponse`; add `tags?: string[]` to `CreatePostRequest`/`UpdatePostRequest`.
  Add `sort?: 'latest' | 'popular'` (and keep `category?`, `search?`, `page?`, `size?`) to
  the `listPublished` params type.
- `src/api/posts.ts`: pass `sort` through to the `GET /posts` query params (same axios
  `{ params }` pattern already used for `category`/`search`).

### 2. Discovery page — `src/views/PostsListView.vue`
This is the home page (`/`) and already has search + category chips + pagination; extend it:
- **Sort toggle**: "Latest" / "Most Popular" segmented control; default `latest`. Changing
  it refetches with `sort` and resets to page 0. Existing debounced search and category
  selection stay; combine all three into the query.
- **Technology filter** = the existing category chips (Java/Spring/Vue/DevOps) — relabel the
  section "Technology" if desired; no new dimension.
- **Tags**: render each post's tags as small chips on the card; clicking a tag sets the
  search box to that tag (reuses the existing search path, which now matches tags server-side).
- Keep loading skeletons, empty state, and pagination.

### 3. Components
- `src/components/PostCard.vue`: show `viewCount` (e.g. "👁 1.2k views") and render `tags`
  as chips. Keep existing category chip, excerpt clamp, author/date.
- `src/views/PostEditorView.vue`: add a **tags input** (comma/enter-separated → `string[]`),
  wired into create/update payloads. Reuse `FormField` + `AppInput` and the
  `useFormValidation` composable for light validation.

### 4. Styling
Follow the existing dark, GitHub-inspired tokens already in the views (bg `#0D1117` /
`#161B22`, border `#30363D`, gold accent `#E6A817`, JetBrains Mono headers). Tag chips and
the sort toggle should reuse the category-chip styling for consistency. Optionally invoke the
`frontend-design` skill for the sort/tag visual treatment.

### Frontend tests (Vitest + Vue Test Utils)
- `src/test/api/posts.test.ts`: `listPublished` forwards `sort`/`category`/`search` params.
- `src/test/views/PostsListView.test.ts` (new): switching the sort toggle refetches with
  `sort=popular`; clicking a tag chip drives a search; category chip filters by category.
- `src/test/components/PostCard.test.ts` (new or extend): renders `viewCount` and `tags`.
- `src/test/views/PostEditorView` test: tags input serializes to `string[]` in the payload.

---

## API contract (source of truth for both sides)

- `GET /api/posts?category={slug}&search={text}&sort={latest|popular}&page&size`
  → `Page<PostSummaryResponse>` where each item now includes `tags: string[]` and
  `viewCount: number`.
- `GET /api/posts/{slug}` → `PostResponse` (now includes `tags`, `viewCount`; the read
  increments `viewCount`).
- `POST /api/posts` / `PUT /api/posts/{id}` accept optional `tags: string[]`.

---

## Verification (end-to-end)

**Backend** (from `backend/devblog/devblog/`):
1. `mvnw.cmd test` — all new/updated unit, slice, repo, and integration tests pass.
2. `mvnw.cmd spring-boot:run`, then against a fresh DB:
   - `GET /api/posts?sort=latest` and `?sort=popular` return different orderings.
   - `GET /api/posts?search=<word in a post body>` and `?search=<a seed tag>` return matches.
   - `GET /api/posts?category=spring` filters by technology.
   - Hit `GET /api/posts/{slug}` twice; confirm `viewCount` increments.
   - Confirm seed posts exist and every author is the default `user`.

**Frontend** (from `frontend/`):
3. `npm test` — Vitest suite green; `npm run lint` (type-check) clean.
4. `npm run dev` (with backend running): on `/`, exercise the sort toggle, technology/category
   chips, search by content and by tag, tag-chip click, and verify view counts render on cards.