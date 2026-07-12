-- V2 upgrades: reusable question bank via reuse-by-reference.
-- Repurpose `questions` as the per-institute bank: it no longer belongs to one test.

CREATE TABLE test_question_references (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id     BIGINT      NOT NULL REFERENCES institutes (id),
    test_id          BIGINT      NOT NULL REFERENCES tests (id),
    bank_question_id BIGINT      NOT NULL REFERENCES questions (id),
    section_id       BIGINT,     -- FK added in V8; null = ungrouped
    position         INTEGER     NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_test_question_ref UNIQUE (test_id, bank_question_id)
);
CREATE INDEX idx_tqr_institute ON test_question_references (institute_id);
CREATE INDEX idx_tqr_test ON test_question_references (test_id);
CREATE INDEX idx_tqr_bank_question ON test_question_references (bank_question_id);

-- Backfill: every existing question becomes a reference from its current test,
-- preserving question ids (and thus attempt_answers linkage) without data loss.
INSERT INTO test_question_references (institute_id, test_id, bank_question_id, position, created_at)
SELECT institute_id, test_id, id, id, now() FROM questions;

-- The bank question no longer stores its owning test.
ALTER TABLE questions DROP COLUMN test_id;
