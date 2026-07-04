-- Vidyapeet initial schema (PostgreSQL).
-- Every tenant-scoped table carries institute_id and is indexed on it.

CREATE TABLE institutes (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    slug          VARCHAR(255) NOT NULL UNIQUE,
    logo_url      VARCHAR(1024),
    primary_color VARCHAR(16),
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id  BIGINT REFERENCES institutes (id),
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_users_institute_email UNIQUE (institute_id, email)
);
CREATE INDEX idx_users_institute ON users (institute_id);

CREATE TABLE batches (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id BIGINT       NOT NULL REFERENCES institutes (id),
    name         VARCHAR(255) NOT NULL,
    description  VARCHAR(1000),
    created_at   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_batches_institute ON batches (institute_id);

CREATE TABLE batch_students (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id BIGINT      NOT NULL REFERENCES institutes (id),
    batch_id     BIGINT      NOT NULL REFERENCES batches (id),
    student_id   BIGINT      NOT NULL REFERENCES users (id),
    created_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_batch_student UNIQUE (batch_id, student_id)
);
CREATE INDEX idx_batch_students_institute ON batch_students (institute_id);

CREATE TABLE notes (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id BIGINT       NOT NULL REFERENCES institutes (id),
    batch_id     BIGINT       NOT NULL REFERENCES batches (id),
    subject      VARCHAR(255) NOT NULL,
    title        VARCHAR(255) NOT NULL,
    file_url     VARCHAR(1024) NOT NULL,
    file_size    BIGINT,
    uploaded_by  BIGINT REFERENCES users (id),
    created_at   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_notes_institute ON notes (institute_id);
CREATE INDEX idx_notes_batch ON notes (batch_id);

CREATE TABLE tests (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id     BIGINT       NOT NULL REFERENCES institutes (id),
    batch_id         BIGINT       NOT NULL REFERENCES batches (id),
    title            VARCHAR(255) NOT NULL,
    duration_minutes INTEGER      NOT NULL,
    total_marks      INTEGER      NOT NULL DEFAULT 0,
    is_published     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_tests_institute ON tests (institute_id);
CREATE INDEX idx_tests_batch ON tests (batch_id);

CREATE TABLE questions (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id   BIGINT        NOT NULL REFERENCES institutes (id),
    test_id        BIGINT        NOT NULL REFERENCES tests (id),
    text           VARCHAR(2000) NOT NULL,
    option_a       VARCHAR(1024) NOT NULL,
    option_b       VARCHAR(1024) NOT NULL,
    option_c       VARCHAR(1024) NOT NULL,
    option_d       VARCHAR(1024) NOT NULL,
    correct_option VARCHAR(1)    NOT NULL,
    marks          INTEGER       NOT NULL DEFAULT 1,
    created_at     TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_questions_institute ON questions (institute_id);
CREATE INDEX idx_questions_test ON questions (test_id);

CREATE TABLE test_attempts (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id BIGINT      NOT NULL REFERENCES institutes (id),
    test_id      BIGINT      NOT NULL REFERENCES tests (id),
    student_id   BIGINT      NOT NULL REFERENCES users (id),
    score        INTEGER     NOT NULL DEFAULT 0,
    status       VARCHAR(16) NOT NULL,
    started_at   TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_attempt_test_student UNIQUE (test_id, student_id)
);
CREATE INDEX idx_attempts_institute ON test_attempts (institute_id);
CREATE INDEX idx_attempts_test ON test_attempts (test_id);

CREATE TABLE attempt_answers (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id    BIGINT      NOT NULL REFERENCES institutes (id),
    attempt_id      BIGINT      NOT NULL REFERENCES test_attempts (id),
    question_id     BIGINT      NOT NULL REFERENCES questions (id),
    selected_option VARCHAR(1),
    is_correct      BOOLEAN     NOT NULL DEFAULT FALSE,
    marks_awarded   INTEGER     NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_answers_institute ON attempt_answers (institute_id);
CREATE INDEX idx_answers_attempt ON attempt_answers (attempt_id);
