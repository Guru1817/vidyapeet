-- V2 upgrades: allow an optional image to be attached to a question.

ALTER TABLE questions ADD COLUMN image_key VARCHAR(255);
