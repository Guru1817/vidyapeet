# Requirements Document

## Introduction

Vidyapeet V2 upgrades extend the live multi-tenant coaching-center SaaS ("Shopify for coaching centers") with two tracks of work delivered on the existing stack (Java 17 / Spring Boot 3.2.5 / Spring Data JPA / Spring Security JWT / Flyway / PostgreSQL in prod, H2 in dev; React 18 + Vite + Tailwind frontend; Render + Vercel + Supabase hosting, all free tier).

**Track A — Marketing & UI:** a code-based SVG brand logo, an SEO-friendly public landing page at the apex domain with student sign-up and login for all roles, and a persistent dark mode available to logged-out and logged-in users.

**Track B — Exam engine upgrades:** per-institute reusable question bank (reuse-by-reference), question images stored in Supabase Storage, and labeled test sections under a single overall timer.

All work MUST preserve three platform invariants: multi-tenant isolation (Hibernate `@Filter` + `TenantAwareJpaRepository`), the `AnswerCodec` single-source-of-truth for answer encoding/comparison, and Flyway-owned production schema (any schema change ships as a new migration `V5`/`V6`... with `ddl-auto=validate` in prod). All work MUST stay within free-tier limits (Render free, Vercel, Supabase ~1GB storage).

The suggested build order is Track A first (logo → landing page + SEO → dark mode), then Track B (question images → question bank reuse → timed sections). Requirement numbering reflects this sequencing.

## Glossary

- **Platform**: The overall Vidyapeet SaaS system spanning backend, frontend, and hosting.
- **Apex_Domain**: The root domain `vidyapeeth.in` with no institute subdomain; hosts the public landing page.
- **Institute_Subdomain**: A per-institute branded portal at `<slug>.vidyapeeth.in`.
- **Landing_Page**: The public marketing page served only at the Apex_Domain.
- **Backend**: The Spring Boot API service.
- **Frontend**: The React + Vite single-page application.
- **Auth_Service**: The backend authentication component exposing login, register, and `/api/auth/me`.
- **Theme_Service**: The backend component that persists and returns a logged-in user's theme preference.
- **Theme_Toggle**: The frontend control that switches between light and dark appearance.
- **User_Summary**: The `/api/auth/me` response payload (`UserSummary` record) returned to the Frontend.
- **Brand_Logo**: The code-based SVG logo (wordmark + icon) used across the Platform.
- **Brand_Theming**: The per-institute CSS-variable color theming (`--brand`, `--brand-dark`) driven by `BrandingContext`.
- **SEO_Assets**: The collection of on-page and technical SEO deliverables (meta tags, Open Graph tags, JSON-LD, `sitemap.xml`, `robots.txt`, semantic HTML, prerendered landing markup).
- **Prerenderer**: The build-time or serving-time mechanism that produces crawlable static HTML for the Landing_Page.
- **Question_Bank**: A per-institute collection of reusable questions not owned by a single test.
- **Bank_Question**: A question stored in the Question_Bank that may be referenced by many tests.
- **Test_Question_Reference**: The link that attaches a Bank_Question to a MockTest by reference.
- **AnswerCodec**: The existing single source of truth for encoding, decoding, and comparing answers across MCQ, MSQ, TRUE_FALSE, and FILL_BLANK question types.
- **Storage_Service**: The `StorageService` seam (`LocalStorageService` in dev, `SupabaseStorageService` in prod).
- **Question_Image**: An image file attached to a question and stored via Storage_Service.
- **Test_Section**: A labeled, ordered grouping of questions within a single MockTest, used for organization only.
- **Overall_Timer**: The single countdown timer that governs an entire test attempt regardless of sections.
- **Flyway_Migration**: A versioned SQL migration file owning production schema changes.
- **Tenant_Isolation**: The two-layer mechanism (Hibernate `@Filter` + `TenantAwareJpaRepository`) enforcing `institute_id` scoping.
- **Free_Tier**: The combined hosting constraints of Render free, Vercel, and Supabase free (~1GB storage).

## Requirements

### Requirement 1: Code-based SVG brand logo

**User Story:** As the platform owner, I want a clean, scalable, code-based SVG brand logo (wordmark + icon), so that the Platform presents a consistent, editable identity aimed at coaching institutes and students in India.

#### Acceptance Criteria

1. THE Frontend SHALL render the Brand_Logo as inline or code-based SVG markup that scales without rasterization at any display size.
2. THE Brand_Logo SHALL be composed of a distinct icon element and a "Vidyapeeth" wordmark element that can be rendered together or independently.
3. THE Frontend SHALL display the Brand_Logo in the application header.
4. THE Frontend SHALL display the Brand_Logo on the Landing_Page.
5. THE Frontend SHALL expose the Brand_Logo as the site favicon.
6. WHERE an institute has not configured a custom logo, THE Frontend SHALL use the Brand_Logo as the branding default.
7. THE Brand_Logo SHALL adapt its foreground colors for both light and dark appearance so that it remains legible against each background.
8. WHERE Brand_Theming CSS variables (`--brand`, `--brand-dark`) are active, THE Brand_Logo SHALL render without overriding an institute's configured brand colors.

### Requirement 2: Public landing page at the apex domain

**User Story:** As a prospective student or institute owner, I want a public marketing landing page at vidyapeeth.in, so that I can learn what the Platform offers and sign up or log in.

#### Acceptance Criteria

1. WHEN a visitor requests the Apex_Domain without an authenticated session, THE Frontend SHALL serve the Landing_Page.
2. WHEN a visitor requests an Institute_Subdomain, THE Frontend SHALL serve the institute portal and SHALL NOT serve the Landing_Page.
3. THE Landing_Page SHALL present marketing content describing who the Platform is, what it offers, and its functionalities and features.
4. THE Landing_Page SHALL provide a student sign-up entry point that submits to the existing student registration flow of the Auth_Service.
5. THE Landing_Page SHALL provide a log-in entry point that supports SUPER_ADMIN, INSTITUTE_ADMIN, and STUDENT roles.
6. THE Landing_Page SHALL NOT provide any institute self-sign-up mechanism.
7. THE Landing_Page footer SHALL display the contact email `vidyapeeth.in@gmail.com` for institute sign-up requests and queries.
8. WHEN a student completes sign-up from the Landing_Page, THE Auth_Service SHALL create the account with role STUDENT.

### Requirement 3: SEO-friendly public landing page

**User Story:** As the platform owner, I want the public landing page to follow on-page and technical SEO best practices, so that the site is eligible to be discovered in search engines for the brand term and relevant long-tail phrases.

#### Acceptance Criteria

1. THE Landing_Page SHALL include a unique `<title>` tag and a meta description tag targeting the brand term "vidyapeeth".
2. THE Landing_Page SHALL include Open Graph meta tags for title, description, type, URL, and image.
3. THE Landing_Page SHALL embed JSON-LD structured data describing the organization and the software product.
4. THE Platform SHALL serve a `sitemap.xml` listing the public Landing_Page URL at the Apex_Domain.
5. THE Platform SHALL serve a `robots.txt` that allows crawling of the Landing_Page and disallows crawling of authenticated application routes.
6. THE Landing_Page SHALL use semantic HTML landmark elements (for example `header`, `nav`, `main`, `section`, `footer`) for its primary content structure.
7. THE Prerenderer SHALL produce crawlable static HTML for the Landing_Page so that primary marketing content is present in the initial HTML response without requiring client-side JavaScript execution.
8. THE authenticated application routes SHALL be excluded from prerendering and from the sitemap.
9. THE Landing_Page SHALL include descriptive content for long-tail phrases including "mock test platform for coaching institutes".

### Requirement 4: Persistent dark mode

**User Story:** As any user, I want to toggle dark mode, so that I can use the Platform comfortably, and as a logged-in user I want my choice to follow me across sessions and devices.

#### Acceptance Criteria

1. THE Frontend SHALL provide a Theme_Toggle to both non-logged-in and logged-in users.
2. WHEN a non-logged-in visitor selects a theme, THE Frontend SHALL store the preference client-side in `localStorage`.
3. WHILE a non-logged-in visitor has no stored theme preference, THE Frontend SHALL apply the appearance indicated by the `prefers-color-scheme` media query.
4. WHEN a logged-in user selects a theme, THE Theme_Service SHALL persist the preference server-side against the user's account.
5. THE User_Summary payload returned by `/api/auth/me` SHALL include the logged-in user's persisted theme preference.
6. WHEN a logged-in user loads the Frontend on any device, THE Frontend SHALL apply the theme preference from the User_Summary payload.
7. THE Frontend SHALL apply dark appearance styling using Tailwind `dark:` variants across existing components.
8. WHILE dark mode is active, THE Frontend SHALL preserve Brand_Theming CSS variables (`--brand`, `--brand-dark`) so that institute brand colors are not overridden by the dark theme.
9. THE Theme_Service SHALL introduce the user theme-preference column through a new Flyway_Migration.

### Requirement 5: Question images

**User Story:** As an institute admin, I want to attach an image to a question, so that I can present diagram-based or figure-based questions to students.

#### Acceptance Criteria

1. THE Storage_Service SHALL accept image file uploads in addition to the existing PDF uploads.
2. WHEN an institute admin uploads a Question_Image for a question, THE Backend SHALL store the file via Storage_Service and associate the stored reference with that question.
3. THE Backend SHALL persist the Question_Image reference through a new Flyway_Migration.
4. IF an uploaded file is not a supported image type, THEN THE Backend SHALL reject the upload and return a descriptive error.
5. WHEN an institute admin opens the test editor for a question that has a Question_Image, THE Frontend SHALL render the Question_Image.
6. WHEN a student takes a test containing a question that has a Question_Image, THE Frontend SHALL render the Question_Image in the take-test view.
7. WHEN a student views a result for a question that has a Question_Image, THE Frontend SHALL render the Question_Image in the result view.
8. THE Backend SHALL enforce Tenant_Isolation on Question_Image storage and retrieval so that an institute can access only its own images.
9. THE Backend SHALL enforce a per-image size limit that keeps total stored files within the Free_Tier Supabase storage budget.

### Requirement 6: Reusable question bank across tests

**User Story:** As an institute admin, I want a shared per-institute question bank whose questions can be attached to many tests by reference, so that editing a bank question updates every test that references it.

#### Acceptance Criteria

1. THE Backend SHALL provide a per-institute Question_Bank of Bank_Questions scoped by `institute_id`.
2. WHEN an institute admin attaches a Bank_Question to a MockTest, THE Backend SHALL create a Test_Question_Reference rather than copying the question content.
3. THE Backend SHALL allow a single Bank_Question to be referenced by multiple tests simultaneously.
4. WHEN an institute admin edits a Bank_Question, THE Backend SHALL reflect the change in every test that holds a Test_Question_Reference to that Bank_Question.
5. THE Backend SHALL encode and compare all Bank_Question answers exclusively through AnswerCodec for MCQ, MSQ, TRUE_FALSE, and FILL_BLANK types.
6. THE Backend SHALL enforce Tenant_Isolation so that Bank_Questions and Test_Question_References are accessible only within their owning institute.
7. THE Backend SHALL migrate the existing `question.test_id` data model to support reuse-by-reference through a new Flyway_Migration without losing existing questions.
8. WHEN a student submits an attempt for a test that references Bank_Questions, THE Backend SHALL grade and record per-question answers and scores using the referenced Bank_Question definitions.
9. WHEN an institute admin removes a Test_Question_Reference from a MockTest, THE Backend SHALL detach the Bank_Question from that test while retaining the Bank_Question in the Question_Bank.
10. THE Backend SHALL preserve DOUBLE-typed scores for attempts on tests that reference Bank_Questions.

### Requirement 7: Timed sections with a single overall timer

**User Story:** As an institute admin, I want to organize a test into labeled sections while the whole test runs on one overall timer, so that I can group questions without managing separate section timers.

#### Acceptance Criteria

1. THE Backend SHALL allow a MockTest to define zero or more labeled, ordered Test_Sections.
2. WHEN an institute admin assigns a question to a Test_Section, THE Backend SHALL record the section grouping for that question within the test.
3. THE Backend SHALL persist Test_Section structure through a new Flyway_Migration.
4. THE Backend SHALL govern each test attempt with a single Overall_Timer derived from the test's duration and SHALL NOT enforce per-section time limits.
5. WHEN a student takes a sectioned test, THE Frontend SHALL display questions grouped under their Test_Section labels.
6. WHILE a student is taking a sectioned test, THE Frontend SHALL display the single Overall_Timer for the whole test.
7. WHEN the Overall_Timer reaches zero, THE Frontend SHALL auto-submit the attempt regardless of section.
8. WHERE a MockTest defines no Test_Sections, THE Frontend SHALL present the test as an ungrouped list of questions.
9. THE Backend SHALL enforce Tenant_Isolation on Test_Section data so that sections are accessible only within their owning institute.

### Requirement 8: Platform invariants and free-tier constraint

**User Story:** As the platform owner, I want all V2 changes to preserve existing platform invariants and stay within free-tier limits, so that production stability, tenant isolation, and grading correctness are never compromised.

#### Acceptance Criteria

1. THE Platform SHALL preserve two-layer Tenant_Isolation (Hibernate `@Filter` and `TenantAwareJpaRepository`) for every new tenant-scoped table introduced by V2.
2. THE Platform SHALL route all answer encoding and comparison for new and existing question features through AnswerCodec.
3. WHERE a V2 feature changes the production schema, THE Platform SHALL introduce the change through a new Flyway_Migration and SHALL keep `ddl-auto` set to `validate` in the production profile.
4. THE Platform SHALL keep total Supabase Storage usage within the Free_Tier budget of approximately 1GB.
5. THE Platform SHALL operate on Render free, Vercel, and Supabase free tiers without requiring a paid hosting upgrade.
6. THE Platform SHALL preserve DOUBLE-typed scores across all attempt and grading changes introduced by V2.
