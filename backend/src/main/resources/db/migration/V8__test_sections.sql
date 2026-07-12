-- V2 upgrades: labeled, ordered test sections under a single overall timer.
-- Sections are organizational only; they add no per-section timing.

CREATE TABLE test_sections (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id BIGINT       NOT NULL REFERENCES institutes (id),
    test_id      BIGINT       NOT NULL REFERENCES tests (id),
    label        VARCHAR(255) NOT NULL,
    position     INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_test_sections_institute ON test_sections (institute_id);
CREATE INDEX idx_test_sections_test ON test_sections (test_id);

-- Wire the reference's section grouping to the new table
-- (test_question_references.section_id was created nullable in V7).
ALTER TABLE test_question_references
    ADD CONSTRAINT fk_tqr_section FOREIGN KEY (section_id) REFERENCES test_sections (id);
