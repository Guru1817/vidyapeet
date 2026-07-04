-- Phase 3: content library (folders, files) and sharing files/tests to batches.

CREATE TABLE library_folders (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id BIGINT       NOT NULL REFERENCES institutes (id),
    name         VARCHAR(255) NOT NULL,
    description  VARCHAR(1000),
    created_at   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_library_folders_institute ON library_folders (institute_id);

CREATE TABLE library_files (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id BIGINT        NOT NULL REFERENCES institutes (id),
    folder_id    BIGINT        NOT NULL REFERENCES library_folders (id),
    subject      VARCHAR(255)  NOT NULL,
    title        VARCHAR(255)  NOT NULL,
    file_url     VARCHAR(1024) NOT NULL,
    file_size    BIGINT,
    uploaded_by  BIGINT REFERENCES users (id),
    created_at   TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_library_files_institute ON library_files (institute_id);
CREATE INDEX idx_library_files_folder ON library_files (folder_id);

-- Tests can now live in a library folder; batch is optional.
ALTER TABLE tests ALTER COLUMN batch_id DROP NOT NULL;
ALTER TABLE tests ADD COLUMN folder_id BIGINT REFERENCES library_folders (id);

CREATE TABLE batch_tests (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id BIGINT      NOT NULL REFERENCES institutes (id),
    batch_id     BIGINT      NOT NULL REFERENCES batches (id),
    test_id      BIGINT      NOT NULL REFERENCES tests (id),
    created_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_batch_test UNIQUE (batch_id, test_id)
);
CREATE INDEX idx_batch_tests_institute ON batch_tests (institute_id);

CREATE TABLE batch_library_files (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    institute_id    BIGINT      NOT NULL REFERENCES institutes (id),
    batch_id        BIGINT      NOT NULL REFERENCES batches (id),
    library_file_id BIGINT      NOT NULL REFERENCES library_files (id),
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_batch_library_file UNIQUE (batch_id, library_file_id)
);
CREATE INDEX idx_batch_library_files_institute ON batch_library_files (institute_id);
