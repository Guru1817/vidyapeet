# Vidyapeet — Multi-Tenant Learning Platform for Coaching Centers

"Shopify for coaching centers": one codebase and one database serve many coaching
centers (tenants). Each center's students log in to a portal branded with that
center's name, logo, and color, where they read notes and take auto-graded mock
tests with a per-batch leaderboard.

## Tech stack

- **Backend:** Java 17, Spring Boot 3.2, Spring Data JPA, Spring Security (JWT),
  Apache POI (Excel import), Flyway (PostgreSQL), H2 (local/dev).
- **Frontend:** React 18 + Vite + Tailwind CSS, React Router, Axios.
- **Database:** PostgreSQL in production; H2 in-memory for local dev and tests.

Everything runs on free hosting tiers (Render / Railway / Supabase / Vercel).

## Multi-tenancy (how isolation is enforced)

Isolation is enforced at the **data-access layer**, not just in controllers:

1. The JWT embeds `userId`, `role`, and `instituteId`.
2. `JwtAuthenticationFilter` reads the token and primes a per-request
   `TenantContext` (thread-local). SUPER_ADMIN runs with scoping bypassed.
3. `TenantFilterAspect` enables a Hibernate `@Filter` on the active transactional
   session before every repository call, appending `WHERE institute_id = :tenantId`
   to all queries automatically.
4. `TenantAwareJpaRepository` (the Spring Data base class) closes a Hibernate gap:
   `@Filter` does not apply to primary-key loads, so `findById`/`existsById` for
   tenant entities are routed through a filtered query. One tenant can never read
   another tenant's row, even by guessing an id.

This is covered by `TenantIsolationTest`.

## Project structure

```
vidyapeet/
├── backend/   Spring Boot API (controller -> service -> repository, DTOs)
└── frontend/  React + Vite + Tailwind SPA
```

## Prerequisites

- **JDK 17+** (e.g. Eclipse Temurin)
- **Maven 3.9+**
- **Node.js 20+** and npm

## Run locally (free, no external services)

### 1. Backend (H2 in-memory, demo data seeded)

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080` with the `dev` profile (H2 + seed data).

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

The app starts on `http://localhost:5173` and proxies `/api` to the backend.

### 3. Open the demo portal

- Student/admin portal (branded): `http://localhost:5173/login?tenant=demo`
- Platform owner: `http://localhost:5173/login` (leave the institute code blank)

### Demo logins

| Role            | Institute code | Email                      | Password       |
|-----------------|----------------|----------------------------|----------------|
| Platform owner  | _(blank)_      | superadmin@vidyapeet.app   | superadmin123  |
| Institute admin | demo           | admin@demo.test            | admin12345     |
| Student         | demo           | student1@demo.test         | student123     |

(Also `student2@demo.test`, `student3@demo.test`, same password.)

## Tests

```bash
cd backend
mvn test
```

Includes unit tests for the grading logic (`GraderTest`) and the tenant-scoping
filter (`TenantIsolationTest`).

## Bulk question import (Excel)

In the test editor, upload an `.xlsx` with these columns (first row is a header):

| Question | Option A | Option B | Option C | Option D | Correct (A-D) | Marks |
|----------|----------|----------|----------|----------|---------------|-------|

## Branding

Each institute stores `name`, `logo_url`, `primary_color`, and a unique `slug`.
The frontend resolves the slug from the **subdomain** in production
(`center.vidyapeet.com`) and from `?tenant=<slug>` locally, then loads
`GET /api/branding/{slug}` to theme the portal (logo + primary color via CSS
variables) before login.

## Production notes

- Activate the `prod` Spring profile and set `DATABASE_URL`, `DATABASE_USERNAME`,
  `DATABASE_PASSWORD`, and `VIDYAPEET_JWT_SECRET` (Base64, 256-bit). Flyway manages
  the PostgreSQL schema (`db/migration/V1__init.sql`).
- Set `VITE_API_BASE_URL` for the frontend if the API is on a different origin.
- **File storage:** local disk is used in dev via the `StorageService` seam.
  For production (ephemeral free-tier disks), implement a Supabase Storage
  `StorageService` behind the same interface; notes are served through an
  access-controlled download endpoint either way.

## Key API endpoints

| Area        | Endpoint                                         | Role            |
|-------------|--------------------------------------------------|-----------------|
| Auth        | `POST /api/auth/login`, `/register`, `GET /me`   | public / all    |
| Branding    | `GET /api/branding/{slug}`                        | public          |
| Institutes  | `GET/POST/PUT /api/institutes`                    | SUPER_ADMIN     |
| Batches     | `/api/batches`, `/api/batches/{id}/students`      | INSTITUTE_ADMIN |
| Students    | `GET/POST /api/students`                          | INSTITUTE_ADMIN |
| Notes       | `POST /api/notes`, `GET /api/notes/{id}/download` | admin / student |
| Tests       | `/api/tests`, `/api/tests/{id}/questions/import`  | INSTITUTE_ADMIN |
| Take test   | `POST /api/student/tests/{id}/start`, `.../submit`| STUDENT         |
| Results     | `GET /api/student/tests/{id}/result`              | STUDENT         |
| Leaderboard | `GET /api/tests/{id}/leaderboard`                 | admin / student |
```
