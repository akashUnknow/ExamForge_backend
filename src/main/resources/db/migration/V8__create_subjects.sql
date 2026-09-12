-- Phase 3: subjects, each belonging to exactly one exam

CREATE TABLE subjects (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id        UUID NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    name           VARCHAR(150) NOT NULL,
    description    VARCHAR(500),
    display_order  INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    deleted_at     TIMESTAMPTZ,
    CONSTRAINT uq_subjects_exam_name UNIQUE (exam_id, name)
);

CREATE INDEX idx_subjects_exam ON subjects (exam_id);
