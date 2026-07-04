-- Phase 2: generalize questions to MCQ / MSQ / TRUE_FALSE / FILL_BLANK.

-- Questions: add type + canonical correct answer; options become optional.
ALTER TABLE questions ADD COLUMN type VARCHAR(16) NOT NULL DEFAULT 'MCQ';
ALTER TABLE questions ADD COLUMN correct_answer VARCHAR(2000);

-- Migrate existing single-letter answers into the canonical column.
UPDATE questions SET correct_answer = correct_option WHERE correct_answer IS NULL;

ALTER TABLE questions ALTER COLUMN correct_answer SET NOT NULL;
ALTER TABLE questions ALTER COLUMN option_a DROP NOT NULL;
ALTER TABLE questions ALTER COLUMN option_b DROP NOT NULL;
ALTER TABLE questions ALTER COLUMN option_c DROP NOT NULL;
ALTER TABLE questions ALTER COLUMN option_d DROP NOT NULL;
ALTER TABLE questions DROP COLUMN correct_option;

-- Attempt answers: store the canonical selected answer instead of a single option.
ALTER TABLE attempt_answers ADD COLUMN selected_answer VARCHAR(2000);
UPDATE attempt_answers SET selected_answer = selected_option;
ALTER TABLE attempt_answers DROP COLUMN selected_option;
