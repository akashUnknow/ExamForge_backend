-- Phase 5: tests

CREATE TABLE tests (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id           UUID NOT NULL REFERENCES exams(id) ON DELETE RESTRICT,
    name              VARCHAR(200) NOT NULL,
    description       TEXT,
    duration_minutes  INTEGER      NOT NULL,
    total_marks       NUMERIC(8,2) NOT NULL,
    negative_marking  NUMERIC(5,2) NOT NULL DEFAULT 0,
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    is_free           BOOLEAN      NOT NULL DEFAULT TRUE,
    price             NUMERIC(10,2),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    deleted_at        TIMESTAMPTZ
);

CREATE INDEX idx_tests_exam   ON tests (exam_id);
CREATE INDEX idx_tests_status ON tests (status) WHERE deleted_at IS NULL;
