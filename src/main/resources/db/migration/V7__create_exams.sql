-- Phase 3: exams

CREATE TABLE exams (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id       UUID REFERENCES exam_categories(id) ON DELETE SET NULL,
    name              VARCHAR(200) NOT NULL,
    description       TEXT,
    duration_minutes  INTEGER      NOT NULL,
    total_questions   INTEGER      NOT NULL,
    maximum_marks     NUMERIC(8,2) NOT NULL,
    negative_marking  NUMERIC(5,2) NOT NULL DEFAULT 0,
    difficulty        VARCHAR(20)  NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    deleted_at        TIMESTAMPTZ
);

CREATE INDEX idx_exams_category ON exams (category_id);
CREATE INDEX idx_exams_status   ON exams (status) WHERE deleted_at IS NULL;
CREATE INDEX idx_exams_name     ON exams (name);
