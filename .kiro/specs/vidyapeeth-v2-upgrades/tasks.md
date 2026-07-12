# Implementation Plan: Vidyapeet V2 Upgrades

## Overview

This plan converts the V2 design into incremental coding steps across the existing stack (Java 17 / Spring Boot backend, React 18 + Vite frontend). Work follows the design build order: shared test infrastructure first, then Track A (brand logo → landing page + SEO → dark mode), then Track B (question images → reusable question bank → timed sections), closing with cross-cutting invariant checks.

Each schema change ships as a new Flyway migration (V5–V8) and the prod profile keeps `ddl-auto=validate`. New tenant-scoped tables extend `TenantBaseEntity`, and all answer encoding/comparison continues to route through `AnswerCodec`. Property-based tests (jqwik on the backend, fast-check on the frontend) validate the 12 correctness properties; each property is its own sub-task and is marked optional (`*`).

## Tasks

- [x] 1. Set up property-based test infrastructure
  - [x] 1.1 Add jqwik test dependency to the backend
    - Add jqwik (JUnit 5 platform) as a `test`-scoped dependency in `backend/pom.xml`
    - Confirm the Surefire/JUnit platform picks up jqwik test containers
    - _Requirements: 8.1, 8.2_
  - [x] 1.2 Add fast-check and Vitest configuration to the frontend
    - Add `fast-check` and `vitest` (with jsdom + testing-library) as dev dependencies in `frontend/package.json`
    - Add a `vitest` config and a `test` script so property and component tests can run
    - _Requirements: 2.1_

- [x] 2. Brand logo (Track A)
  - [x] 2.1 Implement the BrandLogo React component
    - Create `frontend/src/components/BrandLogo.jsx` rendering inline `<svg>` markup
    - Support `variant` (`'full' | 'icon' | 'wordmark'`) and `className` props; use `currentColor` for the foreground so it adapts to light/dark and never sets `--brand`/`--brand-dark`
    - _Requirements: 1.1, 1.2, 1.7, 1.8_
  - [x] 2.2 Write unit/snapshot tests for BrandLogo
    - Assert each variant renders the expected icon/wordmark sub-elements and snapshot the SVG output
    - Assert rendering the logo leaves `--brand`/`--brand-dark` untouched
    - _Requirements: 1.1, 1.2, 1.7, 1.8_
  - [x] 2.3 Integrate the logo into header, landing default, and favicon
    - Render `<BrandLogo />` in the portal header/layout and use it as the branding default whenever `branding.logoUrl` is absent
    - Add `frontend/public/favicon.svg` and reference it from `index.html`
    - _Requirements: 1.3, 1.4, 1.5, 1.6_

- [x] 3. Public landing page and SEO (Track A)
  - [x] 3.1 Implement the resolveView(host, isAuthenticated) helper
    - Create a pure helper (e.g. `frontend/src/routing/resolveView.js`) returning `LANDING` only for the bare apex (ignoring `www`) and unauthenticated sessions, otherwise `PORTAL`
    - _Requirements: 2.1, 2.2_
  - [x] 3.2 Write property test for view resolution
    - **Property 1: Apex-vs-portal view resolution**
    - **Validates: Requirements 2.1, 2.2**
    - Use fast-check to assert `LANDING` iff bare apex + unauthenticated; institute subdomains always `PORTAL` regardless of auth
  - [x] 3.3 Implement the LandingPage component
    - Create `frontend/src/pages/LandingPage.jsx` using semantic landmarks (`header`, `nav`, `main`, `section`, `footer`) and marketing copy (who/what/features + long-tail phrase "mock test platform for coaching institutes")
    - Add a student sign-up control posting to `POST /api/auth/register` and a log-in control using `POST /api/auth/login` (all roles); no institute self-sign-up; footer shows `vidyapeeth.in@gmail.com`
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 2.7, 3.9_
  - [x] 3.4 Wire App routing to serve the landing page at the apex only
    - Update `App` to render `LandingPage` only when `resolveView(...) === 'LANDING'`; institute subdomains and authenticated sessions render the SPA
    - _Requirements: 2.1, 2.2_
  - [x] 3.5 Write unit/snapshot tests for landing content
    - Assert marketing copy, long-tail phrase, semantic landmarks, sign-up→register and login wiring, absence of institute self-sign-up, and footer email
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 2.7, 3.6, 3.9_
  - [x] 3.6 Add on-page and static SEO assets
    - Inject `<title>`, meta description (brand term "vidyapeeth"), Open Graph tags (title/description/type/url/image), and JSON-LD (`Organization` + `SoftwareApplication`) into the landing markup
    - Add `frontend/public/sitemap.xml` (apex landing URL only) and `frontend/public/robots.txt` (allow landing, disallow `/admin`, `/student`, `/superadmin`, `/login`)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.8_
  - [x] 3.7 Add the build-time prerender step for the landing route
    - Configure a Vite prerender plugin to emit crawlable static HTML for the apex landing route only; exclude authenticated app routes from prerendering
    - _Requirements: 3.7, 3.8_
  - [x] 3.8 Write prerender output integration test
    - Build the frontend and assert prerendered landing HTML contains marketing content without executing JS, and that `sitemap.xml`/`robots.txt` are emitted with correct allow/disallow rules
    - _Requirements: 3.4, 3.5, 3.7_
  - [x] 3.9 Write property test for student sign-up role
    - **Property 2: Student sign-up always creates a STUDENT**
    - **Validates: Requirements 2.8**
    - jqwik test over valid registration payloads asserting the created account has role `STUDENT`

- [x] 4. Persistent dark mode (Track A)
  - [x] 4.1 Add V5 Flyway migration for the user theme preference
    - Create `V5__user_theme_preference.sql`: `ALTER TABLE users ADD COLUMN theme_preference VARCHAR(8) NOT NULL DEFAULT 'LIGHT'`
    - _Requirements: 4.9, 8.3_
  - [x] 4.2 Add theme preference to User, UserSummary, and Theme_Service endpoint
    - Add `themePreference` (`enum ThemePreference { LIGHT, DARK }`, `@Enumerated(STRING)`, default `LIGHT`) to `User`; add the field to `UserSummary` and return it from `GET /api/auth/me`
    - Add `PUT /api/auth/me/theme` (authenticated) that validates the value and persists it against the current user
    - _Requirements: 4.4, 4.5_
  - [x] 4.3 Write property test for theme persistence round-trip
    - **Property 3: Theme preference persistence round-trip**
    - **Validates: Requirements 4.4, 4.5**
    - jqwik test: persist any `{LIGHT, DARK}` value then reload `UserSummary` and assert equality
  - [x] 4.4 Implement ThemeContext on the frontend
    - Create `frontend/src/theme/ThemeContext.jsx` wrapping `App` above `BrandingContext`; resolution order: logged-in `themePreference` → `localStorage.theme` → `prefers-color-scheme`
    - Toggle the `dark` class on `document.documentElement`; guests write to `localStorage`, logged-in users write to `localStorage` and call `PUT /api/auth/me/theme`; never mutate `--brand`/`--brand-dark`
    - _Requirements: 4.1, 4.2, 4.3, 4.6_
  - [x] 4.5 Enable Tailwind class dark mode and apply dark: variants
    - Set `darkMode: 'class'` in the Tailwind config and add `dark:` variants across existing components while preserving brand CSS variables
    - _Requirements: 4.7, 4.8_
  - [x] 4.6 Write dark mode UI tests
    - Test toggle presence in both auth states, guest `localStorage` persistence, `prefers-color-scheme` fallback, application of server theme on load, and preservation of brand variables when dark is active
    - _Requirements: 4.1, 4.2, 4.3, 4.6, 4.7, 4.8_

- [x] 5. Checkpoint - Track A complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Question images (Track B)
  - [x] 6.1 Add V6 Flyway migration for question images
    - Create `V6__question_image.sql`: `ALTER TABLE questions ADD COLUMN image_key VARCHAR(255)`
    - _Requirements: 5.3, 8.3_
  - [x] 6.2 Widen the StorageService contract to accept images
    - Add `String store(MultipartFile file, Set<String> allowedContentTypes, long maxBytes)`; keep existing `store(file)` delegating with the PDF policy so current callers are untouched
    - Image policy: `image/png`, `image/jpeg`, `image/webp`, per-image cap (e.g. 2 MB); derive the stored key extension from content type; reject disallowed/oversized files with a descriptive error and no key
    - _Requirements: 5.1, 5.4, 5.9, 8.4_
  - [x] 6.3 Write property test for storage type and size policy
    - **Property 4: Storage type and size policy**
    - **Validates: Requirements 5.1, 5.4, 5.9, 8.4**
    - jqwik test over varied content types and sizes (including boundaries and non-image types) using an in-memory/mock StorageService
  - [x] 6.4 Add imageKey to Question and implement QuestionImageController
    - Add `imageKey` to the `Question` entity
    - Implement `POST /api/questions/{id}/image` (INSTITUTE_ADMIN, validates + stores + sets `image_key`), `GET /api/questions/{id}/image` (authenticated, streams with correct media type), `DELETE /api/questions/{id}/image` (INSTITUTE_ADMIN, clears key + best-effort delete)
    - _Requirements: 5.2, 5.8_
  - [x] 6.5 Write property test for image association round-trip
    - **Property 5: Question image association round-trip**
    - **Validates: Requirements 5.2**
    - jqwik test: after upload, `image_key` equals the returned storage key and that key streams back the stored image
  - [x] 6.6 Render question images in editor, take-test, and result views
    - Render the image whenever `image_key` is present in the test editor, take-test view, and result view
    - _Requirements: 5.5, 5.6, 5.7_
  - [x] 6.7 Write unit tests for image rendering
    - Assert editor/take/result views render the image when `image_key` is present and omit it otherwise
    - _Requirements: 5.5, 5.6, 5.7_

- [x] 7. Reusable question bank (Track B)
  - [x] 7.1 Add V7 Flyway migration for reuse-by-reference
    - Create `V7__question_bank_references.sql`: create `test_question_references`, backfill one reference per existing question preserving question ids, then `ALTER TABLE questions DROP COLUMN test_id`
    - _Requirements: 6.7, 8.3_
  - [x] 7.2 Repurpose Question as bank entity and add reference entity/repository
    - Remove `testId` from `Question` (it becomes the institute-scoped bank); create `TestQuestionReference` (`@Entity` extending `TenantBaseEntity`: `testId`, `bankQuestionId`, `sectionId` nullable, `position`) and `TestQuestionReferenceRepository` with `findByTestIdOrderBySectionPositionAscPositionAsc`, `findByBankQuestionId`, `deleteByTestIdAndBankQuestionId`, and a resolved-questions query
    - _Requirements: 6.1, 6.3, 6.6_
  - [x] 7.3 Implement QuestionBankService CRUD and attach/detach endpoints
    - Bank CRUD edits questions in place (no content copy); `POST /api/tests/{id}/references` creates a reference; `DELETE /api/tests/{id}/references` removes only the reference row; `GET/POST/PUT/DELETE /api/bank/questions`; keep answer encoding/comparison through `AnswerCodec`
    - _Requirements: 6.1, 6.2, 6.4, 6.5, 6.9_
  - [x] 7.4 Write property test for attaching references without copying
    - **Property 6: Attaching references without copying**
    - **Validates: Requirements 6.2, 6.3**
  - [x] 7.5 Write property test for edit propagation
    - **Property 7: Editing a bank question reflects in every referencing test**
    - **Validates: Requirements 6.4**
  - [x] 7.6 Write property test for detach retaining the bank question
    - **Property 9: Detaching a reference retains the bank question**
    - **Validates: Requirements 6.9**
  - [x] 7.7 Update the grading path to resolve questions by reference
    - Change `TakeTestService` to resolve a test's questions via `TestQuestionReferenceRepository` (ordered by section position then reference position) instead of `findByTestId`; keep `Grader`, `GradeOutcome`, `AttemptAnswer`, and DOUBLE-typed scores unchanged
    - _Requirements: 6.5, 6.8, 6.10, 8.2, 8.6_
  - [x] 7.8 Write property test for grading via referenced definitions
    - **Property 8: Grading via referenced definitions**
    - **Validates: Requirements 6.5, 6.8, 6.10, 8.2, 8.6**
  - [x] 7.9 Write migration integration test for V7 backfill
    - Run V7 against a seeded PostgreSQL-mode database and assert the reference count equals the prior question count, question ids are preserved (attempt_answers linkage holds), and no questions are lost
    - _Requirements: 6.7_

- [x] 8. Timed sections (Track B)
  - [x] 8.1 Add V8 Flyway migration for test sections
    - Create `V8__test_sections.sql`: create `test_sections` and add the `fk_tqr_section` FK from `test_question_references.section_id`
    - _Requirements: 7.3, 8.3_
  - [x] 8.2 Add TestSection entity and repository
    - Create `TestSection` (`@Entity` extending `TenantBaseEntity`: `testId`, `label`, `position`) and `TestSectionRepository` with `findByTestIdOrderByPositionAsc`; ensure `section_id` on the reference is populated when grouping
    - _Requirements: 7.1, 7.2, 7.9_
  - [x] 8.3 Implement section management endpoints
    - Add create/rename/reorder/delete section endpoints (`/api/tests/{id}/sections`) and assign-reference-to-section; keep the overall timer derived from `startedAt + durationMinutes` with no per-section limits
    - _Requirements: 7.1, 7.2, 7.4_
  - [x] 8.4 Write property test for section grouping and ordering
    - **Property 10: Section grouping and ordering**
    - **Validates: Requirements 7.2, 7.5, 7.8**
  - [x] 8.5 Implement section-grouped take-test and editor views
    - Group questions under their section labels in the editor and take-test views; render one ungrouped list when the test has no sections; show the single overall timer and auto-submit at zero
    - _Requirements: 7.5, 7.6, 7.7, 7.8_
  - [x] 8.6 Write property test for overall timer derivation
    - **Property 11: Overall timer derivation is independent of sections**
    - **Validates: Requirements 7.4**
  - [x] 8.7 Write unit tests for ungrouped fallback and timer UI
    - Assert ungrouped rendering with no sections and single overall-timer display/auto-submit behavior
    - _Requirements: 7.6, 7.7, 7.8_

- [x] 9. Cross-cutting invariants and free-tier posture (Req 8)
  - [x] 9.1 Write property test for tenant isolation across new V2 tables
    - **Property 12: Tenant isolation across all new V2 tables**
    - **Validates: Requirements 5.8, 6.1, 6.6, 7.9, 8.1**
    - Generate two distinct institutes and assert no cross-tenant visibility via repository queries and `findById` for bank questions, references, sections, and images
  - [x] 9.2 Add prod schema-validation and free-tier smoke tests
    - Boot the app under the `prod` profile against the migrated schema to confirm `ddl-auto=validate` passes with no entity/schema drift; smoke check that no configuration requires paid resources
    - _Requirements: 8.3, 8.5_

- [x] 10. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional test tasks and can be skipped for a faster MVP, but they validate the design's correctness properties and invariants.
- Backend property tests use jqwik; the frontend `resolveView` property test uses fast-check. Each property test runs a minimum of 100 iterations and is tagged `Feature: vidyapeeth-v2-upgrades, Property {number}: {property_text}`.
- Storage property tests use an in-memory/mock `StorageService` so iterations never touch Supabase.
- Each task references specific requirement sub-clauses for traceability; checkpoints ensure incremental validation.
- All schema changes ship as new Flyway migrations (V5–V8) and prod keeps `ddl-auto=validate`; new tenant-scoped tables extend `TenantBaseEntity`; answer handling stays through `AnswerCodec`.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "2.1", "3.1"] },
    { "id": 1, "tasks": ["2.2", "3.2", "3.3", "3.9", "4.1", "6.1"] },
    { "id": 2, "tasks": ["2.3", "3.5", "3.6", "4.2", "6.2"] },
    { "id": 3, "tasks": ["3.4", "3.7", "4.3", "6.3", "6.4"] },
    { "id": 4, "tasks": ["3.8", "4.4", "6.5", "6.6", "7.1"] },
    { "id": 5, "tasks": ["4.5", "6.7", "7.2"] },
    { "id": 6, "tasks": ["4.6", "7.3"] },
    { "id": 7, "tasks": ["7.4", "7.5", "7.6", "7.7"] },
    { "id": 8, "tasks": ["7.8", "7.9", "8.1"] },
    { "id": 9, "tasks": ["8.2"] },
    { "id": 10, "tasks": ["8.3", "8.5"] },
    { "id": 11, "tasks": ["8.4", "8.6", "8.7"] },
    { "id": 12, "tasks": ["9.1", "9.2"] }
  ]
}
```
