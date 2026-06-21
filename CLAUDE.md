# Developer Blog Platform

A web application for software developers to create, manage, and share technical blog
posts organized by technology and framework categories — tutorials, best practices,
project experiences, and technical insights. Goal: a centralized hub for publishing and
discovering technical content to foster knowledge sharing across technology communities.

## Non-Functional Requirements

- Responsive UI for web browsers and mobile devices
- Secure authentication and authorization (JWT-based)
- Scalable architecture supporting future feature expansion
- RESTful API for frontend–backend communication
- Optimized database queries and performance

## Technology Stack

### Backend
- Java 21
- Spring Boot 4 (Web MVC, Validation, JSON)
- Spring Security 6 (stateless JWT)
- Spring Data JPA + Hibernate (SQLite community dialect)
- SQLite (`sqlite-jdbc`)
- JWT (`io.jsonwebtoken` / jjwt)
- Lombok

> QueryDSL is named as an aspiration in the original stack but is **not** currently
> wired into `pom.xml`; persistence uses plain Spring Data JPA today.

### Frontend
- Vue 3 (Composition API, `<script setup>`)
- TypeScript
- Vite
- Tailwind CSS 4
- Pinia (state) + Vue Router
- Axios (API client)
- markdown-it + highlight.js + DOMPurify (Markdown rendering & sanitization)
- Vitest + Vue Test Utils (testing)

## Repository Layout

```
develop_blog/
├── backend/devblog/               # Spring Boot Maven project (package: met.ivan.devblog)
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd              # Maven wrapper
│   └── src/
│       ├── main/java/met/ivan/devblog/
│       │   ├── config/             # Security, app properties, data seeding
│       │   ├── controller/         # REST controllers + global exception handler
│       │   ├── dto/                # Request/response DTOs (API boundary)
│       │   ├── entity/             # JPA entities (User, Role, Post, Category, RefreshToken, Comment, PostLike, PostBookmark)
│       │   ├── exception/          # Domain exceptions
│       │   ├── mapper/             # Entity ⇄ DTO mappers
│       │   ├── repository/         # Spring Data JPA repositories
│       │   ├── security/           # JWT service, auth filter, user details service
│       │   ├── service/            # Business logic (interfaces + impl)
│       │   └── util/               # Helpers (e.g. slug generation)
│       ├── main/resources/application.yaml
│       └── test/                   # Unit, slice, and integration tests
├── frontend/                       # Vue 3 + TypeScript SPA
│   ├── package.json
│   ├── vite.config.ts              # Vite config + /api dev proxy
│   └── src/
│       ├── api/                    # Axios client, typed endpoints (auth, posts, …)
│       ├── components/             # Reusable UI components
│       ├── composables/           # Reusable composition functions
│       ├── router/                 # Vue Router + route guards
│       ├── stores/                 # Pinia stores (auth, …)
│       ├── views/                  # Page-level components (incl. admin/)
│       └── test/                   # Vitest tests
└── docs/                           # Plans (docs/plans) + prompt logs (docs/prompt_logs)
```

> Note: the Maven project lives at `backend/devblog/`. Run backend commands from
> that directory.

## Build & Run

### Backend (from `backend/devblog/`)
- Run: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`) — serves on `:8080`
- Test: `./mvnw test`
- Package: `./mvnw clean package`

### Frontend (from `frontend/`)
- Install: `npm install`
- Dev server: `npm run dev` — serves on `:5173`, proxies `/api` to `:8080`
- Build: `npm run build`
- Test: `npm test`
- Lint (type-check): `npm run lint`

## Current State

Both halves are implemented and functional:
- **Backend** — Spring Boot 4 REST API (Java 21, package `met.ivan.devblog`) with Spring
  Security + JWT auth (access/refresh tokens), Spring Data JPA over SQLite, BCrypt
  password hashing, CORS for the SPA, and a global exception handler. Domains: users,
  roles, posts (Markdown, draft/published lifecycle, slugs, free-form tags, and a
  `viewCount` popularity counter), and categories. Public post listing supports filtering
  by category, search across title/content/tags, and `sort=latest|popular`; reading a
  post increments its view count. Engagement features layer on top: flat comments on
  published posts (`/api/posts/{slug}/comments`, `/api/comments/{id}`), idempotent likes
  and private bookmarks (`/api/posts/{slug}/like|bookmark`,
  `/api/users/me/bookmarks`), and public author profiles (`/api/authors/{username}` and
  `/{username}/posts`). Post payloads are enriched with `likeCount` (and `liked`/
  `bookmarked` for the authenticated caller). An Administration area (role `ADMIN`, all
  under `/api/admin/**`) adds comment moderation (global comment list
  `GET /api/admin/comments` + hard-delete via the existing comment endpoint), content
  management (`GET /api/admin/posts` across all authors/statuses, reusing status-change/
  delete), platform statistics (`GET /api/admin/stats`: headline totals + top posts by
  views/likes + recent users), alongside the existing category and user management
  endpoints. On startup `DataInitializer` seeds roles, a
  default admin/user, starter categories, starter posts (authored by the default user),
  and starter comments/likes for content discovery.
- **Frontend** — Vue 3 + TypeScript SPA (Pinia, Vue Router with auth guards, typed Axios
  client) covering auth, profile, post authoring, public browsing, and admin views. The
  admin area is unified under an `/admin` dashboard hub (platform stats + nav) with comment
  moderation (`/admin/comments`), content management (`/admin/posts`), plus the existing
  user (`/admin/users`) and category (`/admin/categories`) management views.
- **Config/secrets** — `application.yaml` exposes env-overridable settings (`JWT_SECRET`,
  token expirations, `CORS_ALLOWED_ORIGINS`, `SEED_*` credentials).
- Tests exist on both sides (backend unit/slice/integration; frontend Vitest).

See `README.md` for the full REST API reference, configuration table, and domain model.

## Conventions

- Backend base package: `met.ivan.devblog`. Organize by feature/layer
  (e.g. `controller`, `service`, `repository`, `entity`, `dto`, `config`, `security`).
- Expose functionality as RESTful endpoints under the `/api` base path.
- Use DTOs at the API boundary; do not expose JPA entities directly.
- Lombok is available — prefer it for boilerplate (getters/setters/builders).
- Keep secrets (JWT signing keys, etc.) out of source; use config/env, not hardcoded values.

## Working With Agents

This project is intended to be built with two specialized agents:
- **Backend agent** — owns everything under `backend/`.
- **Frontend agent** — owns everything under `frontend/`.

The contract between them is the REST API. When changing API shape (endpoints, request/
response DTOs, auth flow), keep both sides in sync and treat the API as the source of truth.