# Vidyapeet — Full Project Handoff Prompt

## What this project is

**Vidyapeet** is a multi-tenant SaaS web application — "Shopify for coaching centers." A single codebase and single database serve many coaching institutes (tenants). Each institute gets its own branded learning portal where students log in, access study materials, and take auto-graded mock tests with a leaderboard.

---

## Tech stack (do not change)

- **Backend:** Java 17, Spring Boot 3.2.5, Spring Data JPA, Spring Security with JWT, Apache POI (Excel import), Flyway migrations, Maven
- **Database:** PostgreSQL (production), H2 in-memory (local dev/tests — resets on restart by design)
- **Frontend:** React 18 + Vite + Tailwind CSS, React Router v6, Axios
- **Build:** Maven (backend), npm (frontend)
- **File storage:** StorageService seam with a LocalStorageService implementation for dev; Supabase Storage planned for production
- **Free hosting targets:** Render/Railway (backend), Vercel (frontend), Supabase (database + storage)

---

## Project location on disk

```
C:\Users\gbtri\IdeaProjects\Vidyapeeth\
├── backend\        (Spring Boot)
├── frontend\       (React + Vite)
└── README.md
```

---

## Three roles

| Role | Who | What they can do |
|---|---|---|
| SUPER_ADMIN | Platform owner (you) | Create, edit, delete coaching centers (institutes). No institute affiliation. |
| INSTITUTE_ADMIN | Coaching center owner | Manage students, batches, notes, tests, library, performance. |
| STUDENT | Enrolled student | View notes, take tests, see results, check performance. |

---

## Multi-tenancy approach (critical — do not break)

- Shared database with institute_id on every tenant-scoped table.
- TenantContext — ThreadLocal holding the current institute_id per request.
- JwtAuthenticationFilter — reads JWT, primes TenantContext. SUPER_ADMIN bypasses scoping.
- TenantFilterAspect — enables a Hibernate @Filter on every repository call, adding WHERE institute_id = :tenantId automatically.
- TenantAwareJpaRepository — custom Spring Data base class; routes findById/existsById through a filtered query to close a Hibernate gap (filters do not apply to PK loads by default).
- Entities extend TenantBaseEntity (carries institute_id, auto-stamped by TenantEntityListener).
- Institute and User are NOT tenant-filtered at the DB layer (auth must resolve them before tenancy is known).

---

## Package structure (backend)

```
com.vidyapeet
├── VidyapeetApplication.java
├── attempt/            take-test, grading, results, leaderboard
├── auth/               JWT login, register, /me
├── batch/              batches + enrollment
├── config/             JpaConfig, DataSeeder (demo data)
├── common/             Role, exception handling (ApiException, GlobalExceptionHandler)
├── dashboard/          DashboardController (admin counts)
├── exam/               MockTest, Question, QuestionType, AnswerCodec, TestType, ExamService, ExamController
├── institute/          Institute, InstituteService, InstituteController, BrandingController
├── library/            LibraryFolder, LibraryFile, BatchLibraryFile, LibraryService, LibraryController
├── note/               Note, NoteService, NoteController, StudentNoteController
├── performance/        PerformanceService, PerformanceController
├── security/           JwtService, JwtAuthenticationFilter, SecurityConfig, UserPrincipal, SecurityUtils
├── storage/            StorageService (interface), LocalStorageService
├── tenant/             TenantContext, TenantAware, TenantBaseEntity, TenantEntityListener, TenantAwareJpaRepository, TenantFilterAspect
└── user/               User, StudentAdminService, StudentAdminController
```

---

## Data model (current state after all four phases)

```
institutes
  id, name, slug (UNIQUE), logo_url, primary_color, created_at

users
  id, institute_id (NULL for SUPER_ADMIN), name, email, password_hash,
  role (SUPER_ADMIN|INSTITUTE_ADMIN|STUDENT), description, created_at
  UNIQUE (institute_id, email)

batches
  id, institute_id, name, description, created_at

batch_students
  id, institute_id, batch_id, student_id
  UNIQUE (batch_id, student_id)

notes
  id, institute_id, batch_id, subject, title, file_url, file_size,
  uploaded_by, created_at

tests
  id, institute_id, batch_id (nullable), folder_id (nullable), title,
  duration_minutes, total_marks, is_published, test_type (EXAM|PRACTICE),
  negative_marking, negative_mark_per_wrong, created_at
  NOTE: batch_id set for batch-native tests; folder_id set for library tests

questions
  id, institute_id, test_id, type (MCQ|MSQ|TRUE_FALSE|FILL_BLANK),
  text, option_a, option_b, option_c, option_d (nullable for TF/Fill),
  correct_answer (canonical string — see AnswerCodec below), marks, created_at

test_attempts
  id, institute_id, test_id, student_id, score (DOUBLE), status
  (IN_PROGRESS|SUBMITTED), started_at, submitted_at, created_at
  NOTE: no unique constraint — PRACTICE allows multiple attempts
  NOTE: EXAM one-attempt rule is enforced in TakeTestService.start(), not in DB

attempt_answers
  id, institute_id, attempt_id, question_id,
  selected_answer (canonical string, nullable), is_correct,
  marks_awarded (DOUBLE), created_at

library_folders
  id, institute_id, name, description, created_at

library_files
  id, institute_id, folder_id, subject, title, file_url, file_size,
  uploaded_by, created_at

batch_library_files
  id, institute_id, batch_id, library_file_id
  UNIQUE (batch_id, library_file_id)

batch_tests
  id, institute_id, batch_id, test_id
  UNIQUE (batch_id, test_id)
```

---

## Answer encoding — AnswerCodec (important for grading)

All question types store answers in one canonical column (correct_answer / selected_answer):

| Type | Encoding example |
|---|---|
| MCQ | "B" |
| MSQ | "A,C" (sorted, comma-joined) |
| TRUE_FALSE | "TRUE" or "FALSE" |
| FILL_BLANK | "newton|newtons" (pipe-separated accepted answers) |

The Grader uses AnswerCodec.isCorrect() for all types. MSQ is all-or-nothing. Fill-blank is case-insensitive. Unanswered questions are never penalised (even with negative marking on).

---

## Flyway migrations (production schema versioning)

```
V1__init.sql                        full initial schema
V2__phase1_students_tests.sql       description, test_type, negative_marking, decimal scores, drop attempt unique constraint
V3__phase2_question_types.sql       type column, correct_answer column, drop correct_option, rename selected_option
V4__phase3_library.sql              library_folders, library_files, batch_tests, batch_library_files, batch_id nullable on tests
```

H2 dev uses ddl-auto: create-drop — it regenerates on every restart by design. Do not try to fix this.

---

## All implemented features (what is DONE)

### Auth
- POST /api/auth/login — slug-scoped for admin/student, no-slug for SUPER_ADMIN
- POST /api/auth/register — student self-registration, role forced to STUDENT
- GET /api/auth/me
- JWT is stateless; embeds userId, role, instituteId

### Branding
- GET /api/branding/{slug} — public; frontend loads institute name, logo, color before login
- Frontend BrandingContext reacts to the logged-in user: owner shows "Vidyapeet" default; admin/student see their own institute branding
- Slug resolved from subdomain in production, ?tenant= query param for local dev

### SUPER_ADMIN — Institute management
- POST /api/institutes — creates center plus first INSTITUTE_ADMIN account atomically
- GET /api/institutes, GET /api/institutes/{id}, PUT /api/institutes/{id}
- DELETE /api/institutes/{id} — full cascade delete of all tenant data

### INSTITUTE_ADMIN — Dashboard
- GET /api/admin/dashboard — counts of students, batches, tests, notes

### INSTITUTE_ADMIN — Student management
- POST /api/students — create student with optional description field
- GET /api/students, PUT /api/students/{id}, DELETE /api/students/{id}
- Delete cascades: attempt answers -> attempts -> enrollments -> user row

### INSTITUTE_ADMIN — Batches and enrollment
- POST /api/batches, GET /api/batches, GET /api/batches/{id}, PUT /api/batches/{id}
- POST /api/batches/{id}/students — enroll student
- DELETE /api/batches/{id}/students/{studentId} — unenroll
- GET /api/batches/{id}/students — list enrolled students
- POST /api/batches/{id}/library-files/{fileId} — assign library file to batch
- DELETE /api/batches/{id}/library-files/{fileId} — unassign
- GET /api/batches/{id}/library-files — list assigned files
- POST /api/batches/{id}/library-tests/{testId} — assign library test to batch
- DELETE /api/batches/{id}/library-tests/{testId} — unassign

### INSTITUTE_ADMIN — Notes (batch-native PDFs)
- POST /api/notes — multipart PDF upload (10 MB limit, PDF only)
- GET /api/notes?batchId= — list notes for a batch
- DELETE /api/notes/{id}
- GET /api/notes/{id}/download — access-controlled (admin or enrolled student)

### INSTITUTE_ADMIN — Tests and Question bank
- POST /api/tests — create with batchId (batch-native) OR folderId (library test); includes testType (EXAM|PRACTICE), negativeMarking, negativeMarkPerWrong
- GET /api/tests?batchId= — lists batch-native tests plus assigned library tests
- GET /api/tests/{id} — full detail with questions
- PUT /api/tests/{id}, DELETE /api/tests/{id}
- POST /api/tests/{id}/questions — add question (MCQ/MSQ/TRUE_FALSE/FILL_BLANK)
- PUT /api/tests/{id}/questions/{qid}, DELETE /api/tests/{id}/questions/{qid}
- POST /api/tests/{id}/questions/import — bulk import from Excel (with Type column)
- GET /api/tests/{id}/leaderboard — available to admin and enrolled students

### INSTITUTE_ADMIN — Library
- POST /api/library/folders, GET /api/library/folders, GET /api/library/folders/{id}
- PUT /api/library/folders/{id}, DELETE /api/library/folders/{id}
- POST /api/library/folders/{id}/files — multipart PDF upload into folder
- DELETE /api/library/files/{id}
- GET /api/library/files/{id}/download — access-controlled

### INSTITUTE_ADMIN — Performance
- GET /api/admin/performance — per-student overview (all students in institute)
- GET /api/admin/students/{id}/performance — individual student detail

### STUDENT — Tests
- GET /api/student/tests — published tests for enrolled batches plus assigned library tests; annotated with attempt status and best score
- POST /api/student/tests/{testId}/start — starts or resumes; EXAM blocks after submitted
- POST /api/student/attempts/{attemptId}/submit — auto-grades, stores per-question answers and score
- GET /api/student/tests/{testId}/result — latest submitted result with full breakdown per question type
- GET /api/student/attempts/{attemptId}/result
- PRACTICE tests allow unlimited re-attempts; leaderboard uses best score

### STUDENT — Notes
- GET /api/student/notes — batch-native notes plus library files assigned to the student's batches

### STUDENT — Performance
- GET /api/student/performance — own attempt history, averages, best score

---

## Frontend page structure

```
src/
├── App.jsx                              routing and nav config
├── main.jsx                             providers: AuthProvider wraps BrandingProvider wraps App
├── auth/
│   ├── AuthContext.jsx                  login, register, logout, /me refresh on load
│   └── ProtectedRoute.jsx               role guard and role-based home routing
├── branding/
│   └── BrandingContext.jsx              slug resolution and CSS variable theming (--brand, --brand-dark)
├── api/
│   └── client.js                        axios with JWT interceptor, downloadFile helper, errorMessage helper
├── lib/
│   ├── tenant.js                        slug from subdomain / ?tenant / localStorage
│   └── format.js                        formatScore (decimal-aware, no trailing .0)
├── components/
│   ├── ui.jsx                           Button, Card, CardBody, Field, Input, Select, Textarea, Alert, Badge, EmptyState
│   ├── Spinner.jsx
│   ├── PortalLayout.jsx                 header with branding, nav links, logout
│   └── PerformanceView.jsx              shared stat cards and attempts table
└── pages/
    ├── LoginPage.jsx                    login + student register, branded
    ├── superadmin/
    │   └── InstitutesPage.jsx           create / edit / delete institutes
    ├── admin/
    │   ├── AdminDashboardPage.jsx       stat tiles
    │   ├── StudentsPage.jsx             add / edit / delete students with description
    │   ├── BatchesPage.jsx              batch list and create
    │   ├── BatchDetailPage.jsx          students + notes + tests + library assignment section
    │   ├── TestEditorPage.jsx           settings + all 4 question types + bulk Excel import
    │   ├── LibraryPage.jsx              folder list
    │   ├── LibraryFolderPage.jsx        files and tests inside a folder
    │   ├── AdminPerformancePage.jsx     per-student overview table
    │   └── AdminStudentPerformancePage.jsx  drill-in for one student
    └── student/
        ├── StudentTestsPage.jsx         test list with type badges, status, retake button for PRACTICE
        ├── StudentNotesPage.jsx         notes and library files
        ├── StudentPerformancePage.jsx   own performance view
        ├── TakeTestPage.jsx             timed UI for all 4 question types, auto-submit on timeout
        ├── ResultPage.jsx               answer breakdown per question type
        └── LeaderboardPage.jsx          rank medals, "you" highlight
```

---

## Demo data seeded on every local dev start

| Role | Institute code | Email | Password |
|---|---|---|---|
| SUPER_ADMIN | (blank) | superadmin@vidyapeet.app | superadmin123 |
| INSTITUTE_ADMIN | demo | admin@demo.test | admin12345 |
| STUDENT | demo | student1@demo.test | student123 |
| STUDENT | demo | student2@demo.test | student123 |
| STUDENT | demo | student3@demo.test | student123 |

Institute: Vidyapeet Demo Classes (slug: demo, color: #4F46E5)
Batch: Class 10 - Science with all 3 students enrolled
Notes: 2 PDFs (placeholder URLs)
Test: Physics Mock Test 1 — 4 MCQ questions, 5 total marks, published

---

## How to run locally

```bash
# Prerequisites: JDK 17, Maven 3.9, Node 20
# Maven is at: C:\Users\gbtri\tools\apache-maven-3.9.9\bin\mvn.cmd
# JAVA_HOME is: C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot

# Backend
cd backend
mvn spring-boot:run
# starts on http://localhost:8080 with H2 in-memory and demo data

# Frontend
cd frontend
npm install
npm run dev
# starts on http://localhost:5173 (proxies /api to backend automatically)

# Open in browser
# Student or admin portal: http://localhost:5173/login?tenant=demo
# Platform owner portal:   http://localhost:5173/login  (leave institute code blank)
```

---

## Tests

```bash
cd backend && mvn test

# Results:
# GraderTest        — 7 tests (MCQ, MSQ, TRUE_FALSE, FILL_BLANK, negative marking, unanswered, empty)
# TenantIsolationTest — 3 tests (tenant sees own rows, blocked by id, bypass works)
```

---

## What is NOT done yet — planned next steps

### 1. Supabase Storage (highest priority for production)
The StorageService interface already exists at com.vidyapeet.storage.StorageService.
Create SupabaseStorageService implements StorageService that uploads/downloads via the Supabase Storage REST API.
Required environment variables:
- SUPABASE_URL — your Supabase project URL
- SUPABASE_SERVICE_KEY — service-role key
- SUPABASE_BUCKET — bucket name (create one called vidyapeet-files in Supabase dashboard)
Wire it as @Primary in the prod Spring profile. LocalStorageService stays active for dev.

### 2. Deployment config
Backend on Render or Railway:
- Add a Dockerfile OR rely on Render's Maven auto-detect
- Set PORT env var (already used in application.yml)
- Activate Spring profile: SPRING_PROFILES_ACTIVE=prod
- Required env vars: DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, VIDYAPEET_JWT_SECRET (Base64-encoded 256-bit random string), VIDYAPEET_CORS_ORIGINS (your Vercel frontend URL)
- Flyway will run V1 through V4 migrations automatically on first boot against Supabase Postgres

Frontend on Vercel:
- Set VITE_API_BASE_URL to the Render/Railway backend URL
- Build command: npm run build
- Output directory: dist

### 3. Email notifications (optional — out of scope for v1)
Student registration / test result available notifications.
Spring Mail is not yet added to pom.xml.

### 4. Subdomain routing in production
Currently working via ?tenant=slug locally and subdomain detection in production.
For real subdomains on Vercel you need a wildcard *.yourdomain.com domain and a Vercel rewrite.
The backend tenant resolution already handles subdomain detection.

### 5. Known gaps to address
- The README.md at the project root still describes v1 (MCQ-only, no library, no performance) — needs rewriting.
- No automated integration tests for library, performance, or phase 1/2/3 service logic beyond grading unit tests.
- Excel bulk import template showing the new Type column is not downloadable from the UI.
- The seeded demo test has only MCQ questions — add MSQ/TRUE_FALSE/FILL_BLANK examples to the seeder for a better demo.

---

## Key technical decisions to preserve

1. Hibernate @Filter + TenantAwareJpaRepository — two-layer tenant isolation. Do not bypass either layer.
2. AnswerCodec is the single source of truth for encoding and comparing answers of all question types. Add new types here first.
3. MockTest.batchId is nullable — library tests have folderId instead. Both share the same entity and question/attempt tables.
4. PRACTICE tests allow multiple attempts — no DB unique constraint on (test_id, student_id). EXAM enforcement is in TakeTestService.start(), not the database.
5. Score is DOUBLE — supports negative marking with fractional deductions. Never revert to INTEGER.
6. Flyway owns the production schema — ddl-auto is set to validate in prod. Never use create-drop or update in production.
7. CORS origins and JWT secret are environment-variable driven — never hard-code them.
8. BrandingContext depends on AuthContext — AuthProvider must wrap BrandingProvider in main.jsx (current order). Do not invert.
9. The tenant filter only fires inside an active transaction. All tenant data access goes through @Transactional service methods. Direct repository calls from tests must use TransactionTemplate.
