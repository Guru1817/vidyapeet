package com.vidyapeet.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for the V7 reuse-by-reference backfill migration
 * ({@code V7__question_bank_references.sql}).
 *
 * <p>Validates: Requirements 6.7
 *
 * <h2>Why this shape</h2>
 * The production schema is owned by Flyway (see {@code application-prod.yml}) and runs on
 * PostgreSQL; dev/test disable Flyway and let Hibernate generate the schema, so the
 * migrations are never exercised by the rest of the suite. This test needs a
 * <em>pre-seed-then-migrate</em> harness: a database that holds the pre-V7 schema and
 * data, against which the real V7 SQL is executed and its backfill invariants asserted.
 *
 * <p>The gold-standard harness would run every migration verbatim against a real
 * PostgreSQL (Testcontainers). That is unavailable here: there is no Testcontainers
 * dependency and no Docker runtime on the build host. The next option — replaying V1..V6
 * against H2's PostgreSQL-compatibility mode — is not viable either, because those
 * migrations use several PostgreSQL-only spellings H2 cannot parse
 * ({@code TIMESTAMPTZ}, comma-separated multi-column {@code ADD COLUMN},
 * {@code ALTER COLUMN ... TYPE ... USING ...}). Shimming all of them would make the
 * harness brittle and stop it resembling the real migration.
 *
 * <p>So this test scopes itself to the migration actually under test (V7). It reconstructs
 * the <b>pre-V7 schema</b> for exactly the tables the V7 backfill reads and rewrites — in
 * their real post-V6 shape ({@code institutes}, {@code users}, {@code tests},
 * {@code questions} still owning {@code test_id}, {@code test_attempts},
 * {@code attempt_answers}) — using H2-compatible DDL, seeds representative data, then
 * executes the <b>actual {@code V7__question_bank_references.sql}</b> file. The only shim
 * applied to V7 is normalising the {@code TIMESTAMPTZ} type name in its {@code CREATE
 * TABLE}; the behaviour under test — {@code INSERT INTO test_question_references (...)
 * SELECT institute_id, test_id, id, id, now() FROM questions} and
 * {@code ALTER TABLE questions DROP COLUMN test_id} — runs verbatim.
 *
 * <p><b>Limitations:</b> the pre-V7 schema is reconstructed rather than produced by
 * V1..V6, and V7's {@code TIMESTAMPTZ} spelling is normalised for H2. This test therefore
 * certifies the V7 backfill <i>data invariants</i> (Req 6.7), not PostgreSQL type-name
 * compatibility (covered separately by the prod-profile schema-validation boot).
 *
 * <h2>Key assertions (Req 6.7 — migrate without losing questions)</h2>
 * <ul>
 *   <li>{@code count(test_question_references) == } prior {@code count(questions)}.</li>
 *   <li>Every question id appears exactly once as a {@code bank_question_id}; no question
 *       rows are lost and ids are preserved.</li>
 *   <li>The {@code attempt_answers.question_id} linkage still resolves to existing
 *       {@code questions} rows (ids preserved), so historical attempts stay intact.</li>
 *   <li>Each backfilled reference carries the question's original {@code test_id} and
 *       {@code institute_id}.</li>
 *   <li>{@code questions.test_id} is dropped.</li>
 * </ul>
 */
class V7BackfillMigrationTest {

    /** Pre-V7 schema for the tables the V7 backfill touches, in their real post-V6 shape. */
    private static final String PRE_V7_SCHEMA = """
            CREATE TABLE institutes (
                id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                name          VARCHAR(255) NOT NULL,
                slug          VARCHAR(255) NOT NULL UNIQUE,
                primary_color VARCHAR(16),
                created_at    TIMESTAMP WITH TIME ZONE NOT NULL
            );
            CREATE TABLE users (
                id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                institute_id  BIGINT REFERENCES institutes (id),
                name          VARCHAR(255) NOT NULL,
                email         VARCHAR(255) NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                role          VARCHAR(32)  NOT NULL,
                created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
                CONSTRAINT uk_users_institute_email UNIQUE (institute_id, email)
            );
            CREATE TABLE batches (
                id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                institute_id BIGINT       NOT NULL REFERENCES institutes (id),
                name         VARCHAR(255) NOT NULL,
                created_at   TIMESTAMP WITH TIME ZONE NOT NULL
            );
            CREATE TABLE tests (
                id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                institute_id     BIGINT       NOT NULL REFERENCES institutes (id),
                batch_id         BIGINT       REFERENCES batches (id),
                title            VARCHAR(255) NOT NULL,
                duration_minutes INTEGER      NOT NULL,
                total_marks      INTEGER      NOT NULL DEFAULT 0,
                is_published     BOOLEAN      NOT NULL DEFAULT FALSE,
                created_at       TIMESTAMP WITH TIME ZONE NOT NULL
            );
            CREATE TABLE questions (
                id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                institute_id   BIGINT        NOT NULL REFERENCES institutes (id),
                test_id        BIGINT        NOT NULL REFERENCES tests (id),
                text           VARCHAR(2000) NOT NULL,
                type           VARCHAR(16)   NOT NULL DEFAULT 'MCQ',
                correct_answer VARCHAR(2000) NOT NULL,
                marks          INTEGER       NOT NULL DEFAULT 1,
                image_key      VARCHAR(255),
                created_at     TIMESTAMP WITH TIME ZONE NOT NULL
            );
            CREATE INDEX idx_questions_test ON questions (test_id);
            CREATE TABLE test_attempts (
                id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                institute_id BIGINT           NOT NULL REFERENCES institutes (id),
                test_id      BIGINT           NOT NULL REFERENCES tests (id),
                student_id   BIGINT           NOT NULL REFERENCES users (id),
                score        DOUBLE PRECISION NOT NULL DEFAULT 0,
                status       VARCHAR(16)      NOT NULL,
                started_at   TIMESTAMP WITH TIME ZONE NOT NULL,
                created_at   TIMESTAMP WITH TIME ZONE NOT NULL
            );
            CREATE TABLE attempt_answers (
                id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                institute_id    BIGINT           NOT NULL REFERENCES institutes (id),
                attempt_id      BIGINT           NOT NULL REFERENCES test_attempts (id),
                question_id     BIGINT           NOT NULL REFERENCES questions (id),
                selected_answer VARCHAR(2000),
                marks_awarded   DOUBLE PRECISION NOT NULL DEFAULT 0,
                created_at      TIMESTAMP WITH TIME ZONE NOT NULL
            );
            """;

    @Test
    void v7BackfillsOneReferencePerQuestionPreservingIdsAndLinkage() throws IOException {
        String url = "jdbc:h2:mem:v7backfill_" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, "sa", "");
        dataSource.setDriverClassName("org.h2.Driver");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // 1. Build the pre-V7 schema (real post-V6 shape for the affected tables).
        for (String statement : splitStatements(PRE_V7_SCHEMA)) {
            jdbc.execute(statement);
        }

        // Sanity: pre-V7 the questions table owns test_id and there is no reference table.
        assertThat(columnExists(jdbc, "questions", "test_id"))
                .as("questions.test_id should exist before V7")
                .isTrue();
        assertThat(tableExists(jdbc, "test_question_references"))
                .as("test_question_references should not exist before V7")
                .isFalse();

        // 2. Seed pre-V7 data.
        Long instituteId = insertInstitute(jdbc, "Alpha Academy", "alpha");
        Long studentId = insertUser(jdbc, instituteId, "Student One", "student@alpha.test", "STUDENT");
        Long batchId = insertBatch(jdbc, instituteId, "Batch A");
        Long testId = insertTest(jdbc, instituteId, batchId, "Mock Test 1", 60);

        int questionCount = 7;
        long[] questionIds = new long[questionCount];
        for (int i = 0; i < questionCount; i++) {
            questionIds[i] = insertQuestion(jdbc, instituteId, testId,
                    "Q" + i + ": what is " + i + " + 1?", "A", 4);
        }

        // An attempt with one answer per question, so we can prove the
        // attempt_answers.question_id linkage survives the migration.
        Long attemptId = insertAttempt(jdbc, instituteId, testId, studentId);
        for (long questionId : questionIds) {
            insertAttemptAnswer(jdbc, instituteId, attemptId, questionId, "A");
        }

        // Capture the pre-migration state.
        long priorQuestionCount = jdbc.queryForObject("SELECT COUNT(*) FROM questions", Long.class);
        List<Long> priorQuestionIds = jdbc.queryForList("SELECT id FROM questions ORDER BY id", Long.class);
        long priorAnswerCount = jdbc.queryForObject("SELECT COUNT(*) FROM attempt_answers", Long.class);
        assertThat(priorQuestionCount).isEqualTo(questionCount);

        // 3. Run the ACTUAL V7 backfill migration file (verbatim, bar the TIMESTAMPTZ shim).
        runV7Migration(jdbc);

        // 4a. One reference per prior question — count matches, no questions lost.
        long referenceCount = jdbc.queryForObject("SELECT COUNT(*) FROM test_question_references", Long.class);
        assertThat(referenceCount)
                .as("one reference is backfilled per existing question")
                .isEqualTo(priorQuestionCount);

        long postQuestionCount = jdbc.queryForObject("SELECT COUNT(*) FROM questions", Long.class);
        assertThat(postQuestionCount)
                .as("no questions are lost by the migration")
                .isEqualTo(priorQuestionCount);

        // 4b. Question ids are preserved: each id appears exactly once as a bank_question_id,
        //     and the question rows keep their ids.
        List<Long> bankQuestionIds = jdbc.queryForList(
                "SELECT bank_question_id FROM test_question_references ORDER BY bank_question_id", Long.class);
        assertThat(bankQuestionIds)
                .as("each question id appears exactly once as a bank_question_id")
                .containsExactlyElementsOf(priorQuestionIds)
                .doesNotHaveDuplicates();

        List<Long> postQuestionIds = jdbc.queryForList("SELECT id FROM questions ORDER BY id", Long.class);
        assertThat(postQuestionIds)
                .as("question ids are preserved through the migration")
                .containsExactlyElementsOf(priorQuestionIds);

        // 4c. attempt_answers.question_id linkage still resolves to existing questions.
        long danglingAnswers = jdbc.queryForObject(
                "SELECT COUNT(*) FROM attempt_answers aa "
                        + "WHERE NOT EXISTS (SELECT 1 FROM questions q WHERE q.id = aa.question_id)",
                Long.class);
        assertThat(danglingAnswers)
                .as("every attempt_answers.question_id still points at an existing question")
                .isZero();

        long linkedAnswers = jdbc.queryForObject(
                "SELECT COUNT(*) FROM attempt_answers aa JOIN questions q ON q.id = aa.question_id",
                Long.class);
        assertThat(linkedAnswers)
                .as("all answers stay linked to their (now bank) question")
                .isEqualTo(priorAnswerCount);

        // 4d. Each backfilled reference carries the question's original test_id and institute_id.
        long mismatchedReferences = jdbc.queryForObject(
                "SELECT COUNT(*) FROM test_question_references r "
                        + "WHERE r.test_id <> ? OR r.institute_id <> ?",
                Long.class, testId, instituteId);
        assertThat(mismatchedReferences)
                .as("references preserve the question's original test and institute")
                .isZero();

        // 4e. The bank question no longer stores its owning test.
        assertThat(columnExists(jdbc, "questions", "test_id"))
                .as("questions.test_id is dropped by V7")
                .isFalse();
        assertThatThrownBy(() -> jdbc.queryForList("SELECT test_id FROM questions", Long.class))
                .as("selecting the dropped test_id column fails")
                .isInstanceOf(Exception.class);
    }

    // --- Migration runner ---------------------------------------------------------------

    /**
     * Executes the real {@code V7__question_bank_references.sql} from the classpath, applying
     * only the {@code TIMESTAMPTZ}->ANSI type-name shim required for H2 to parse the
     * {@code CREATE TABLE}. The backfill {@code INSERT ... SELECT} and {@code DROP COLUMN}
     * statements — the behaviour under test — are executed exactly as written.
     */
    private void runV7Migration(JdbcTemplate jdbc) throws IOException {
        byte[] bytes = new ClassPathResource("db/migration/V7__question_bank_references.sql")
                .getInputStream().readAllBytes();
        String raw = new String(bytes, StandardCharsets.UTF_8)
                .replaceAll("(?m)--.*$", "")
                .replaceAll("(?i)\\bTIMESTAMPTZ\\b", "TIMESTAMP WITH TIME ZONE");
        for (String statement : splitStatements(raw)) {
            jdbc.execute(statement);
        }
    }

    private List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        for (String part : script.split(";")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                statements.add(trimmed);
            }
        }
        return statements;
    }

    // --- Seed helpers -------------------------------------------------------------------

    private Long insertInstitute(JdbcTemplate jdbc, String name, String slug) {
        jdbc.update("INSERT INTO institutes (name, slug, primary_color, created_at) VALUES (?, ?, ?, now())",
                name, slug, "#000000");
        return jdbc.queryForObject("SELECT id FROM institutes WHERE slug = ?", Long.class, slug);
    }

    private Long insertUser(JdbcTemplate jdbc, Long instituteId, String name, String email, String role) {
        jdbc.update(
                "INSERT INTO users (institute_id, name, email, password_hash, role, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, now())",
                instituteId, name, email, "hash", role);
        return jdbc.queryForObject("SELECT id FROM users WHERE institute_id = ? AND email = ?",
                Long.class, instituteId, email);
    }

    private Long insertBatch(JdbcTemplate jdbc, Long instituteId, String name) {
        jdbc.update("INSERT INTO batches (institute_id, name, created_at) VALUES (?, ?, now())",
                instituteId, name);
        return jdbc.queryForObject("SELECT id FROM batches WHERE institute_id = ? AND name = ?",
                Long.class, instituteId, name);
    }

    private Long insertTest(JdbcTemplate jdbc, Long instituteId, Long batchId, String title, int durationMinutes) {
        jdbc.update(
                "INSERT INTO tests (institute_id, batch_id, title, duration_minutes, total_marks, is_published, created_at) "
                        + "VALUES (?, ?, ?, ?, 0, FALSE, now())",
                instituteId, batchId, title, durationMinutes);
        return jdbc.queryForObject("SELECT id FROM tests WHERE institute_id = ? AND title = ?",
                Long.class, instituteId, title);
    }

    private Long insertQuestion(JdbcTemplate jdbc, Long instituteId, Long testId, String text,
                                String correctAnswer, int marks) {
        jdbc.update(
                "INSERT INTO questions (institute_id, test_id, text, correct_answer, marks, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, now())",
                instituteId, testId, text, correctAnswer, marks);
        return jdbc.queryForObject(
                "SELECT id FROM questions WHERE institute_id = ? AND test_id = ? AND text = ?",
                Long.class, instituteId, testId, text);
    }

    private Long insertAttempt(JdbcTemplate jdbc, Long instituteId, Long testId, Long studentId) {
        jdbc.update(
                "INSERT INTO test_attempts (institute_id, test_id, student_id, score, status, started_at, created_at) "
                        + "VALUES (?, ?, ?, 0, 'SUBMITTED', now(), now())",
                instituteId, testId, studentId);
        return jdbc.queryForObject(
                "SELECT id FROM test_attempts WHERE institute_id = ? AND test_id = ? AND student_id = ?",
                Long.class, instituteId, testId, studentId);
    }

    private void insertAttemptAnswer(JdbcTemplate jdbc, Long instituteId, Long attemptId, Long questionId,
                                     String selectedAnswer) {
        jdbc.update(
                "INSERT INTO attempt_answers (institute_id, attempt_id, question_id, selected_answer, created_at) "
                        + "VALUES (?, ?, ?, ?, now())",
                instituteId, attemptId, questionId, selectedAnswer);
    }

    // --- Schema introspection helpers ---------------------------------------------------

    private boolean columnExists(JdbcTemplate jdbc, String table, String column) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE UPPER(table_name) = UPPER(?) AND UPPER(column_name) = UPPER(?)",
                Long.class, table, column);
        return count != null && count > 0;
    }

    private boolean tableExists(JdbcTemplate jdbc, String table) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE UPPER(table_name) = UPPER(?)",
                Long.class, table);
        return count != null && count > 0;
    }
}
