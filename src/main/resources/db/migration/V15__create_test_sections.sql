-- Phase 5: test sections (optional grouping within a test, e.g. per-subject sections)

CREATE TABLE test_sections (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id           UUID NOT NULL REFERENCES tests(id) ON DELETE RESTRICT,
    name              VARCHAR(150) NOT NULL,
    duration_minutes  INTEGER,
    question_count    INTEGER      NOT NULL,
    marks             NUMERIC(8,2) NOT NULL,
    display_order     INTEGER      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    deleted_at        TIMESTAMPTZ,
    CONSTRAINT uq_test_sections_test_name UNIQUE (test_id, name)
);

CREATE INDEX idx_test_sections_test ON test_sections (test_id);
