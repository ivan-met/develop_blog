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
- Spring Boot
- Spring Security
- Spring Data JPA + QueryDSL
- SQLite
- JWT

### Frontend
- Vue 3 (Composition API)
- TypeScript
- Tailwind CSS

## Repository Layout

```
develop_blog/
├── backend/devblog/devblog/   # Spring Boot Maven project (package: met.ivan.devblog)
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd         # Maven wrapper
│   └── src/main/java/met/ivan/devblog/DevblogApplication.java
└── frontend/                  # Vue 3 app (not yet created)
```

> Note: the Maven project currently lives at the nested path
> `backend/devblog/devblog/`. Run backend commands from that directory.

## Build & Run

### Backend (from `backend/devblog/devblog/`)
- Run: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)
- Test: `./mvnw test`
- Package: `./mvnw clean package`

### Frontend (from `frontend/`, once scaffolded)
- Install: `npm install`
- Dev server: `npm run dev`
- Build: `npm run build`

## Current State

This is an early-stage scaffold. As of now:
- Backend is a fresh Spring Boot project (Java 21, package `met.ivan.devblog`) with
  Spring Data JPA, Spring Web MVC, SQLite (`sqlite-jdbc`), Lombok, and DevTools wired in.
- **Not yet added** (planned per the stack above): Spring Security, JWT, and QueryDSL.
- Frontend has not been scaffolded yet.
- Domain model, REST API, persistence config, and auth are still to be built.

## Conventions

- Backend base package: `met.ivan.devblog`. Organize by feature/layer
  (e.g. `controller`, `service`, `repository`, `entity`, `dto`, `config`, `security`).
- Expose functionality as RESTful endpoints under a versioned base path (e.g. `/api/...`).
- Use DTOs at the API boundary; do not expose JPA entities directly.
- Lombok is available — prefer it for boilerplate (getters/setters/builders).
- Keep secrets (JWT signing keys, etc.) out of source; use config/env, not hardcoded values.

## Working With Agents

This project is intended to be built with two specialized agents:
- **Backend agent** — owns everything under `backend/`.
- **Frontend agent** — owns everything under `frontend/`.

The contract between them is the REST API. When changing API shape (endpoints, request/
response DTOs, auth flow), keep both sides in sync and treat the API as the source of truth.