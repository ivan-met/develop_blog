# Developer Blog Platform

A full-stack web application that lets software developers create, manage, and share
technical blog posts organized by technology and framework categories — tutorials, best
practices, project experiences, and technical insights. The goal is a centralized hub for
publishing and discovering technical content to foster knowledge sharing across technology
communities.

- **Backend** — Java 21 + Spring Boot REST API with JWT authentication, secured with
  Spring Security, persisted to SQLite via Spring Data JPA.
- **Frontend** — Vue 3 (Composition API) + TypeScript single-page app styled with
  Tailwind CSS, talking to the backend over the REST API.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Repository Layout](#repository-layout)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [Backend](#backend)
  - [Frontend](#frontend)
- [Configuration](#configuration)
- [Seed Data](#seed-data)
- [REST API Reference](#rest-api-reference)
- [Domain Model](#domain-model)
- [Authentication & Authorization](#authentication--authorization)
- [Testing](#testing)
- [Conventions](#conventions)
- [Working With Agents](#working-with-agents)

---

## Features

- **User accounts** — registration, login, JWT-based sessions with refresh-token rotation,
  profile editing, and password change.
- **Blog post authoring** — create, edit, and delete posts written in Markdown, with
  draft/published lifecycle management and auto-generated URL slugs.
- **Public discovery** — browse and read published posts, filter by category, and
  full-text-style search by keyword, with pagination.
- **Categories** — posts are organized under technology categories (Java, Spring, Vue,
  DevOps, …), managed by administrators.
- **Role-based administration** — admins manage users (roles, active status) and
  categories.
- **Secure by default** — stateless JWT auth, BCrypt password hashing, method-level
  authorization, and CORS configured for the SPA origin.
- **Markdown rendering** — the frontend renders Markdown with syntax highlighting and
  sanitizes output to prevent XSS.

---

## Tech Stack

### Backend
- Java 21
- Spring Boot 4 (Web MVC, Validation, JSON)
- Spring Security 6 (stateless JWT)
- Spring Data JPA + Hibernate (SQLite community dialect)
- SQLite (`sqlite-jdbc`)
- JWT (`io.jsonwebtoken` / jjwt 0.12.x)
- Lombok
- Maven (with wrapper)

### Frontend
- Vue 3 (Composition API, `<script setup>`)
- TypeScript
- Vite
- Tailwind CSS 4
- Pinia (state management)
- Vue Router
- Axios (API client)
- markdown-it + highlight.js + DOMPurify (Markdown rendering & sanitization)
- Vitest + Vue Test Utils (testing)

---

## Architecture

```
┌──────────────────────────┐         REST / JSON          ┌──────────────────────────┐
│        Frontend          │  ── HTTP (Bearer JWT) ──────▶ │         Backend          │
│  Vue 3 + TS + Tailwind   │                               │   Spring Boot REST API   │
│  Pinia · Vue Router      │ ◀────────── JSON ──────────── │  Security · JPA · JWT    │
│  Axios API client        │                               │                          │
└──────────────────────────┘                               └────────────┬─────────────┘
                                                                         │ JPA / Hibernate
                                                                         ▼
                                                                  ┌──────────────┐
                                                                  │   SQLite DB  │
                                                                  │  devblog.db  │
                                                                  └──────────────┘
```

The REST API is the contract between the two halves. During development the Vite dev
server proxies `/api` requests to the backend on `http://localhost:8080`, so the SPA and
API appear same-origin to the browser.

---

## Repository Layout

```
develop_blog/
├── backend/devblog/devblog/        # Spring Boot Maven project (package: met.ivan.devblog)
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd              # Maven wrapper
│   └── src/
│       ├── main/java/met/ivan/devblog/
│       │   ├── config/             # Security, app properties, data seeding
│       │   ├── controller/         # REST controllers + global exception handler
│       │   ├── dto/                # Request/response DTOs (API boundary)
│       │   ├── entity/             # JPA entities (User, Role, Post, Category, RefreshToken)
│       │   ├── exception/          # Domain exceptions
│       │   ├── mapper/             # Entity ⇄ DTO mappers
│       │   ├── repository/         # Spring Data JPA repositories
│       │   ├── security/           # JWT service, auth filter, user details service
│       │   ├── service/            # Business logic (interfaces + impl)
│       │   └── util/               # Helpers (e.g. slug generation)
│       ├── main/resources/application.yaml
│       └── test/                   # Unit, slice, and integration tests
│
└── frontend/                       # Vue 3 + TypeScript SPA
    ├── package.json
    ├── vite.config.ts              # Vite config + /api dev proxy
    └── src/
        ├── api/                    # Axios client, typed endpoints (auth, posts, …)
        ├── components/             # Reusable UI components
        ├── composables/            # Reusable composition functions
        ├── router/                 # Vue Router + route guards
        ├── stores/                 # Pinia stores (auth, …)
        ├── views/                  # Page-level components (incl. admin/)
        └── test/                   # Vitest tests
```

> Note: the Maven project lives at the nested path `backend/devblog/devblog/`.
> Run all backend commands from that directory.

---

## Prerequisites

- **Java 21** (JDK) — for the backend. The Maven wrapper (`mvnw`) handles Maven itself.
- **Node.js 18+** and **npm** — for the frontend.
- No separate database install required — SQLite is file-based and created on first run.

---

## Getting Started

Run the backend and frontend in two separate terminals.

### Backend

From `backend/devblog/devblog/`:

```bash
# Run the API (http://localhost:8080)
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run

# Run the tests
./mvnw test                     # Windows: mvnw.cmd test

# Build an executable jar
./mvnw clean package            # Windows: mvnw.cmd clean package
```

On first start the application creates `devblog.db` (SQLite) in the working directory,
auto-creates the schema (`ddl-auto: update`), and seeds default roles, users, and
categories (see [Seed Data](#seed-data)).

### Frontend

From `frontend/`:

```bash
npm install         # install dependencies (first time only)
npm run dev         # start the Vite dev server (http://localhost:5173)
npm run build       # type-check and build for production (outputs to dist/)
npm run preview     # preview the production build locally
npm test            # run the Vitest suite
npm run lint        # type-check only (vue-tsc --noEmit)
```

The dev server proxies `/api/*` to `http://localhost:8080`, so start the backend first
(or alongside) for the SPA to function.

---

## Configuration

Backend configuration lives in
`backend/devblog/devblog/src/main/resources/application.yaml` and is overridable via
environment variables. Keep secrets out of source — supply them through the environment.

| Variable | Default | Description |
| --- | --- | --- |
| `JWT_SECRET` | `change-this-secret-…` | HMAC signing key for JWTs. **Override in production** (must be ≥ 256 bits). |
| `JWT_ACCESS_EXPIRATION` | `900000` (15 min) | Access-token lifetime, in milliseconds. |
| `JWT_REFRESH_EXPIRATION` | `604800000` (7 days) | Refresh-token lifetime, in milliseconds. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated allowed CORS origins. |
| `SEED_ADMIN_USERNAME` | `admin` | Seed admin username. |
| `SEED_ADMIN_EMAIL` | `admin@devblog.local` | Seed admin email. |
| `SEED_ADMIN_PASSWORD` | `Admin@1234` | Seed admin password. **Change for any shared/prod environment.** |
| `SEED_USER_USERNAME` | `user` | Seed regular-user username. |
| `SEED_USER_EMAIL` | `user@devblog.local` | Seed regular-user email. |
| `SEED_USER_PASSWORD` | `User@1234` | Seed regular-user password. |

Other notable settings: BCrypt strength is `12`; the SQLite database file is
`devblog.db`; `open-in-view` is disabled. The database file and `.env` files are
git-ignored.

---

## Seed Data

On startup `DataInitializer` ensures the following exist (all idempotent):

- **Roles:** `ADMIN`, `USER`.
- **Admin user** (holds both `ADMIN` and `USER` roles) — defaults `admin` / `Admin@1234`.
- **Regular user** (holds `USER`) — defaults `user` / `User@1234`.
- **Starter categories:** Java, Spring, Vue, DevOps (only seeded when no categories exist).

> ⚠️ The default credentials are for local development only. Override the `SEED_*`
> variables (and `JWT_SECRET`) before deploying anywhere shared.

---

## REST API Reference

Base path: `/api`. All request/response bodies are JSON. Protected endpoints expect an
`Authorization: Bearer <accessToken>` header. List endpoints are paginated (Spring
`Pageable`; default page size `20`, e.g. `?page=0&size=20&sort=createdAt,desc`).

### Auth — `/api/auth` (public)

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/register` | Register a new account; returns tokens + user. `201 Created`. |
| `POST` | `/login` | Authenticate with username/password; returns tokens + user. |
| `POST` | `/refresh` | Exchange a refresh token for a new access token. |
| `POST` | `/logout` | Invalidate a refresh token. `204 No Content`. |

### Users — `/api/users` (authenticated)

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/me` | Get the current user's profile. |
| `PUT` | `/me` | Update the current user's profile. |
| `PUT` | `/me/password` | Change the current user's password. `204 No Content`. |

### Posts — `/api/posts`

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/` | Public | List published posts. Query: `category`, `search`, pagination. |
| `GET` | `/{slug}` | Public | Get a published post by slug. |
| `GET` | `/mine` | Authenticated | List the caller's own posts. Query: `status`, pagination. |
| `GET` | `/mine/{id}` | Authenticated | Get one of the caller's own posts by id. |
| `POST` | `/` | Authenticated | Create a post. `201 Created`. |
| `PUT` | `/{id}` | Authenticated (owner) | Update a post. |
| `PUT` | `/{id}/status` | Authenticated (owner) | Change a post's status (e.g. publish/unpublish). |
| `DELETE` | `/{id}` | Authenticated (owner) | Delete a post. `204 No Content`. |

### Categories — `/api/categories` (public)

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/` | List all categories. |
| `GET` | `/{slug}` | Get a category by slug. |

### Admin — Users — `/api/admin/users` (role `ADMIN`)

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/` | List users. Query: `search`, pagination. |
| `GET` | `/{id}` | Get a user by id. |
| `PUT` | `/{id}/roles` | Update a user's roles. |
| `PUT` | `/{id}/status` | Enable/disable a user account. |

### Admin — Categories — `/api/admin/categories` (role `ADMIN`)

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/` | Create a category. `201 Created`. |
| `PUT` | `/{id}` | Update a category. |
| `DELETE` | `/{id}` | Delete a category. `204 No Content`. |

Errors are returned in a consistent JSON shape (`ErrorResponse`) via a global exception
handler, with appropriate HTTP status codes (`400`, `401`, `403`, `404`, `409`, …).

---

## Domain Model

- **User** — credentials (username, email, BCrypt password hash), active flag, and a set
  of roles.
- **Role** — `ADMIN` or `USER` (`RoleName` enum).
- **RefreshToken** — persisted refresh tokens supporting rotation and logout.
- **Category** — name, unique slug, and description; groups posts.
- **Post** — title, unique slug, Markdown content, optional excerpt, `status`
  (`DRAFT` / `PUBLISHED` via `PostStatus`), author (`User`), optional category,
  `publishedAt`, and audit timestamps (`createdAt`, `updatedAt`) with optimistic-locking
  `version`. Indexed on slug, status, and author.

---

## Authentication & Authorization

- **Stateless JWT** — no server-side HTTP sessions (`SessionCreationPolicy.STATELESS`).
  A `JwtAuthenticationFilter` validates the `Authorization: Bearer` token on each request.
- **Token flow** — `register`/`login` return an access token (short-lived) plus a refresh
  token (long-lived). Use `/api/auth/refresh` to obtain a new access token and
  `/api/auth/logout` to revoke a refresh token.
- **Passwords** — hashed with BCrypt (strength 12).
- **Authorization rules** (see `SecurityConfig`):
  - `/api/auth/**` — public.
  - `GET /api/posts/**`, `GET /api/categories/**` — public reads.
  - `/api/posts` write operations and `/api/posts/mine/**` — authenticated.
  - `/api/users/**` — authenticated.
  - `/api/admin/**` — requires role `ADMIN` (also enforced at method level via
    `@PreAuthorize`).
- **CORS** — configured for the SPA origin (`CORS_ALLOWED_ORIGINS`, default
  `http://localhost:5173`), allowing credentials and the standard methods/headers.

---

## Testing

- **Backend:** `./mvnw test` (from `backend/devblog/devblog/`). Includes unit tests,
  Spring slice tests (web, JPA, security), and end-to-end integration tests
  (e.g. user-management and blog-post-management flows). Tests run against an in-memory H2
  database (`application-test.yaml`).
- **Frontend:** `npm test` (from `frontend/`). Vitest + Vue Test Utils cover API clients,
  stores, composables, router guards, components, and views.

---

## Conventions

- Backend base package: `met.ivan.devblog`, organized by feature/layer (`controller`,
  `service`, `repository`, `entity`, `dto`, `config`, `security`, …).
- Functionality is exposed as RESTful endpoints under the `/api` base path.
- DTOs are used at the API boundary — JPA entities are never exposed directly.
- Lombok is used for boilerplate (getters/setters/builders).
- Secrets (JWT signing key, seed passwords) are supplied via configuration/environment,
  never hardcoded.
- Frontend uses the `@` alias for `src/`, Composition API with `<script setup>`, Pinia for
  state, and a typed Axios client under `src/api/`.

---

## Working With Agents

This project is built with two specialized agents:

- **Backend agent** — owns everything under `backend/`.
- **Frontend agent** — owns everything under `frontend/`.

The contract between them is the REST API. When changing API shape (endpoints, request/
response DTOs, auth flow), keep both sides in sync and treat the API as the source of
truth.
