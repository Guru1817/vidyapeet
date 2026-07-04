-- Phase 1: student description, test types, negative marking, decimal scores,
-- and allowing multiple attempts (for PRACTICE tests).

-- Optional free-text notes about a student.
ALTER TABLE users ADD COLUMN description VARCHAR(1000);

-- Test type + negative marking.
ALTER TABLE tests
    ADD COLUMN test_type VARCHAR(16) NOT NULL DEFAULT 'EXAM',
    ADD COLUMN negative_marking BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN negative_mark_per_wrong DOUBLE PRECISION NOT NULL DEFAULT 0;

-- Allow multiple attempts per student (PRACTICE); EXAM is enforced in code.
ALTER TABLE test_attempts DROP CONSTRAINT uk_attempt_test_student;

-- Scores become decimal to support fractional/negative marking.
ALTER TABLE test_attempts
    ALTER COLUMN score TYPE DOUBLE PRECISION USING score::double precision;
ALTER TABLE attempt_answers
    ALTER COLUMN marks_awarded TYPE DOUBLE PRECISION USING marks_awarded::double precision;
