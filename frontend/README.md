# Developer Blog Platform — Frontend

The Vue 3 + TypeScript single-page app for the Developer Blog Platform. It talks to the
Spring Boot REST API over `/api`, handling authentication, Markdown post authoring, public
browsing, and administration.

> This is the frontend half of the project. See the [root README](../README.md) for the
> backend, full REST API reference, and overall architecture.

---

## Tech Stack

- **Vue 3** (Composition API, `<script setup>`)
- **TypeScript**
- **Vite** — dev server & build tooling
- **Tailwind CSS 4** — styling (via `@tailwindcss/vite`)
- **Pinia** — state management
- **Vue Router** — routing with auth/admin navigation guards
- **Axios** — typed API client with JWT interceptors
- **markdown-it** + **highlight.js** + **DOMPurify** — Markdown rendering, syntax
  highlighting, and HTML sanitization
- **Vitest** + **Vue Test Utils** — unit/component testing (jsdom environment)

---

## Prerequisites

- **Node.js 18+** and **npm**
- The **backend API** running on `http://localhost:8080` (the dev server proxies `/api`
  to it). See the [root README](../README.md#backend) to start it.

---

## Getting Started

```bash
npm install      # install dependencies (first time only)
npm run dev      # start the Vite dev server → http://localhost:5173
```

Then open http://localhost:5173. With the backend running, you can log in with the seeded
accounts (default `admin` / `Admin@1234` or `user` / `User@1234` — see the root README).

---

## Available Scripts

| Script | Description |
| --- | --- |
| `npm run dev` | Start the Vite dev server (`:5173`) with `/api` proxied to `:8080`. |
| `npm run build` | Type-check (`vue-tsc -b`) and build for production into `dist/`. |
| `npm run preview` | Serve the production build locally for a final check. |
| `npm test` | Run the Vitest suite once. |
| `npm run test:watch` | Run Vitest in watch mode. |
| `npm run lint` | Type-check only (`vue-tsc --noEmit`), no build output. |

---

## Project Structure

```
frontend/
├── index.html              # SPA entry HTML
├── vite.config.ts          # Vite config: Vue + Tailwind plugins, @ alias, /api proxy, Vitest
├── tsconfig*.json          # TypeScript project references
└── src/
    ├── main.ts             # App bootstrap (Pinia, Router, root mount)
    ├── App.vue             # Root component
    ├── style.css           # Global styles / Tailwind entry
    ├── api/                # Axios client + typed endpoint modules
    │   ├── http.ts         #   Axios instance + JWT request/refresh interceptors
    │   ├── types.ts        #   Shared request/response types (API contract)
    │   ├── auth.ts         #   /api/auth  (register, login, refresh, logout)
    │   ├── users.ts        #   /api/users (me, profile, password)
    │   ├── posts.ts        #   /api/posts (public + authoring)
    │   └── categories.ts   #   /api/categories
    ├── components/         # Reusable UI (NavBar, PostCard, MarkdownEditor/Preview, …)
    ├── composables/        # Reusable composition functions (e.g. useFormValidation)
    ├── router/             # Vue Router routes + global navigation guards
    ├── stores/             # Pinia stores (auth)
    ├── views/              # Page-level components
    │   └── admin/          #   Admin-only pages (UsersView, CategoriesView)
    └── test/               # Vitest tests (api, stores, composables, router, components, views)
```

The `@` path alias maps to `src/` (configured in `vite.config.ts` and `tsconfig`).

---

## Routing

Routes are defined in `src/router/index.ts`. Route `meta` drives access control via a
global `beforeEach` guard:

| Path | Name | Access |
| --- | --- | --- |
| `/` | `home` | Public — published posts list |
| `/posts/:slug` | `post-detail` | Public — single post |
| `/login`, `/register` | `login`, `register` | Public (redirects away if already authenticated) |
| `/profile` | `profile` | Authenticated |
| `/posts/new`, `/posts/:id/edit` | `post-new`, `post-edit` | Authenticated — post editor |
| `/me/posts` | `my-posts` | Authenticated — the user's own posts |
| `/admin/users` | `admin-users` | Admin only |
| `/admin/categories` | `admin-categories` | Admin only |
| `*` | — | Fallback → redirects to `/` |

Guard behavior:
- `requiresAuth` routes redirect unauthenticated users to `/login?redirect=<target>`.
- `requiresAdmin` routes load the current user if needed, then redirect non-admins home.
- `redirectIfAuth` (login/register) sends already-authenticated users to `/`.

---

## API & Authentication

- **Base URL** — Axios is configured with `baseURL: '/api'`; in development Vite proxies
  `/api/*` to the backend at `http://localhost:8080` (see `vite.config.ts`).
- **Auth state** — the Pinia `auth` store (`src/stores/auth.ts`) holds the access/refresh
  tokens and current user. Tokens are persisted to `localStorage` (`devblog_access_token`,
  `devblog_refresh_token`) and rehydrated on app load.
- **Request interceptor** — attaches `Authorization: Bearer <accessToken>` to outgoing
  requests.
- **Response interceptor** — on a `401`, transparently calls `/api/auth/refresh` **once**,
  queues concurrent requests during the refresh, retries them with the new token, and logs
  out if the refresh fails. Auth endpoints themselves are excluded from this retry loop.
- **Roles** — `isAdmin` / `isUser` getters derive from the loaded user's roles and gate
  admin UI and routes.

---

## Markdown

Post content is authored and rendered as Markdown:
- `MarkdownEditor.vue` for authoring and `MarkdownPreview.vue` for rendering.
- Rendered with **markdown-it**, code highlighted with **highlight.js**, and the resulting
  HTML sanitized with **DOMPurify** before insertion to prevent XSS.

---

## Testing

Tests live under `src/test/`, mirroring the source tree (api, stores, composables, router,
components, views). Run them with:

```bash
npm test            # single run
npm run test:watch  # watch mode
```

Vitest runs in a `jsdom` environment with globals enabled and `src/test/setup.ts` as the
setup file (see `vite.config.ts`).

---

## Conventions

- Composition API with `<script setup>` and TypeScript throughout.
- Import from `src/` via the `@` alias (e.g. `import { useAuthStore } from '@/stores/auth'`).
- All backend calls go through the typed modules in `src/api/` — don't call `axios`
  directly from components. Keep `src/api/types.ts` in sync with the backend DTOs; the REST
  API is the source of truth.
- Pinia for shared state, composables for reusable reactive logic, Tailwind utility classes
  for styling.
