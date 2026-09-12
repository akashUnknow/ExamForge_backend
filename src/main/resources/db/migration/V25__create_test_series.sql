-- Phase 9: test series - a named, orderable bundle of tests under one exam

CREATE TABLE test_series (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id               UUID NOT NULL REFERENCES exams(id) ON DELETE RESTRICT,
    name                  VARCHAR(200) NOT NULL,
    description           TEXT,
    is_free               BOOLEAN      NOT NULL DEFAULT TRUE,
    price                 NUMERIC(10,2),
    start_date            DATE,
    end_date              DATE,
    access_duration_days  INTEGER,
    status                VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    deleted_at            TIMESTAMPTZ
);

CREATE INDEX idx_test_series_exam   ON test_series (exam_id);
CREATE INDEX idx_test_series_status ON test_series (status) WHERE deleted_at IS NULL;
