-- Per-user theme preference (LIGHT or DARK), defaulting to LIGHT for existing users.
ALTER TABLE users ADD COLUMN theme_preference VARCHAR(8) NOT NULL DEFAULT 'LIGHT';
