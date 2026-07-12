-- ---------------------------------------------------------------------------
-- Prod-equivalent schema for the prod-profile Hibernate `validate` boot test.
--
-- WHAT THIS IS
--   An H2-compatible reconstruction of the CUMULATIVE END STATE of the real
--   PostgreSQL Flyway migrations V1..V8 (see backend/src/main/resources/db/migration).
--   It is derived from the migration files (their net effect after all ADD/DROP
--   COLUMN, CREATE TABLE and type changes), NOT from the JPA entities. Hibernate
--   `ddl-auto=validate` then runs against it, so any drift between the JPA entities
--   and the migrated schema surfaces as a validation failure.
--
-- WHY IT EXISTS (see ProdSchemaValidationTest for the full rationale)
--   Prod runs PostgreSQL and the real migrations use PostgreSQL-only spellings
--   (TIMESTAMPTZ, comma-separated multi-column ADD COLUMN, ALTER COLUMN ... TYPE
--   ... USING ...) that H2 cannot parse, and this environment has no
--   Docker/Testcontainers/PostgreSQL. So the real V1..V6 files cannot be replayed
--   here. This file reproduces their END STATE in H2-parseable DDL so the
--   entity<->schema `validate` check can still run.
--
-- TYPE FIDELITY
--   Hibernate 6 schema validation checks that every mapped table and column
--   exists and that column TYPE CATEGORIES match (bigint / varchar / integer /
--   boolean / double precision / timestamp with time zone). It does not enforce
--   VARCHAR length or nullability, so those need not mirror the entities exactly.
--   The TIMESTAMPTZ of the real migrations maps to TIMESTAMP WITH TIME ZONE here
--   (both report the same JDBC type as Hibernate's mapping for java.time.Instant).
--
-- LIMITATION
--   This schema is reconstructed rather than produced by running V1..V8 verbatim,
--   and it is H2 rather than PostgreSQL. It therefore certifies entity<->schema
--   drift (Req 8.3), not PostgreSQL type-name compatibility of the raw migration
--   files. Keep it in lockstep with the real migrations when the schema changes.
-- ---------------------------------------------------------------------------

-- V1: tenant root
CREATE TABLE institutes (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    slug          VARCHAR(255) NOT NULL UNIQUE,
    logo_url      VARCHAR(1024),
    primary_color VARCHAR(16),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

-- V1 users + V2 description + V5 theme_preference
CREATE TABLE users (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id     BIGINT REFERENCES institutes (id),
    name             VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    role             VARCHAR(32)  NOT NULL,
    description      VARCHAR(1000),
    theme_preference VARCHAR(8)   NOT NULL DEFAULT 'LIGHT',
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_users_institute_email UNIQUE (institute_id, email)
);

CREATE TABLE batches (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id BIGINT       NOT NULL REFERENCES institutes (id),
    name         VARCHAR(255) NOT NULL,
    description  VARCHAR(1000),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE batch_students (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id BIGINT      NOT NULL REFERENCES institutes (id),
    batch_id     BIGINT      NOT NULL REFERENCES batches (id),
    student_id   BIGINT      NOT NULL REFERENCES users (id),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_batch_student UNIQUE (batch_id, student_id)
);

CREATE TABLE notes (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id BIGINT        NOT NULL REFERENCES institutes (id),
    batch_id     BIGINT        NOT NULL REFERENCES batches (id),
    subject      VARCHAR(255)  NOT NULL,
    title        VARCHAR(255)  NOT NULL,
    file_url     VARCHAR(1024) NOT NULL,
    file_size    BIGINT,
    uploaded_by  BIGINT REFERENCES users (id),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

-- V4: content library folders (created before tests so tests.folder_id can reference it)
CREATE TABLE library_folders (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id BIGINT       NOT NULL REFERENCES institutes (id),
    name         VARCHAR(255) NOT NULL,
    description  VARCHAR(1000),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

-- V1 tests + V2 (test_type, negative_marking, negative_mark_per_wrong)
-- + V4 (batch_id nullable, folder_id)
CREATE TABLE tests (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id            BIGINT       NOT NULL REFERENCES institutes (id),
    batch_id                BIGINT       REFERENCES batches (id),
    folder_id               BIGINT       REFERENCES library_folders (id),
    title                   VARCHAR(255) NOT NULL,
    duration_minutes        INTEGER      NOT NULL,
    total_marks             INTEGER      NOT NULL DEFAULT 0,
    is_published            BOOLEAN      NOT NULL DEFAULT FALSE,
    test_type               VARCHAR(16)  NOT NULL DEFAULT 'EXAM',
    negative_marking        BOOLEAN      NOT NULL DEFAULT FALSE,
    negative_mark_per_wrong DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL
);

-- V1 questions - V3 (drop correct_option, add type + correct_answer, options nullable)
-- - V6 (image_key) - V7 (drop test_id: it becomes the per-institute bank)
CREATE TABLE questions (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id   BIGINT        NOT NULL REFERENCES institutes (id),
    type           VARCHAR(16)   NOT NULL DEFAULT 'MCQ',
    text           VARCHAR(2000) NOT NULL,
    option_a       VARCHAR(1024),
    option_b       VARCHAR(1024),
    option_c       VARCHAR(1024),
    option_d       VARCHAR(1024),
    correct_answer VARCHAR(2000) NOT NULL,
    marks          INTEGER       NOT NULL DEFAULT 1,
    image_key      VARCHAR(255),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

-- V1 test_attempts + V2 (score -> double precision, drop uk_attempt_test_student)
CREATE TABLE test_attempts (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id BIGINT           NOT NULL REFERENCES institutes (id),
    test_id      BIGINT           NOT NULL REFERENCES tests (id),
    student_id   BIGINT           NOT NULL REFERENCES users (id),
    score        DOUBLE PRECISION NOT NULL DEFAULT 0,
    status       VARCHAR(16)      NOT NULL,
    started_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

-- V1 attempt_answers + V2 (marks_awarded -> double precision)
-- + V3 (drop selected_option, add selected_answer)
CREATE TABLE attempt_answers (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id    BIGINT           NOT NULL REFERENCES institutes (id),
    attempt_id      BIGINT           NOT NULL REFERENCES test_attempts (id),
    question_id     BIGINT           NOT NULL REFERENCES questions (id),
    selected_answer VARCHAR(2000),
    is_correct      BOOLEAN          NOT NULL DEFAULT FALSE,
    marks_awarded   DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

-- V4: library files + batch sharing join tables
CREATE TABLE library_files (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id BIGINT        NOT NULL REFERENCES institutes (id),
    folder_id    BIGINT        NOT NULL REFERENCES library_folders (id),
    subject      VARCHAR(255)  NOT NULL,
    title        VARCHAR(255)  NOT NULL,
    file_url     VARCHAR(1024) NOT NULL,
    file_size    BIGINT,
    uploaded_by  BIGINT REFERENCES users (id),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE batch_tests (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id BIGINT      NOT NULL REFERENCES institutes (id),
    batch_id     BIGINT      NOT NULL REFERENCES batches (id),
    test_id      BIGINT      NOT NULL REFERENCES tests (id),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_batch_test UNIQUE (batch_id, test_id)
);

CREATE TABLE batch_library_files (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id    BIGINT      NOT NULL REFERENCES institutes (id),
    batch_id        BIGINT      NOT NULL REFERENCES batches (id),
    library_file_id BIGINT      NOT NULL REFERENCES library_files (id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_batch_library_file UNIQUE (batch_id, library_file_id)
);

-- V8: test sections (created before references so the section FK can be added)
CREATE TABLE test_sections (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id BIGINT       NOT NULL REFERENCES institutes (id),
    test_id      BIGINT       NOT NULL REFERENCES tests (id),
    label        VARCHAR(255) NOT NULL,
    position     INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

-- V7 test_question_references + V8 (fk_tqr_section on section_id)
CREATE TABLE test_question_references (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id     BIGINT      NOT NULL REFERENCES institutes (id),
    test_id          BIGINT      NOT NULL REFERENCES tests (id),
    bank_question_id BIGINT      NOT NULL REFERENCES questions (id),
    section_id       BIGINT      REFERENCES test_sections (id),
    position         INTEGER     NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_test_question_ref UNIQUE (test_id, bank_question_id)
);
