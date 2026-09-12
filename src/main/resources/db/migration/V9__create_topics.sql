-- Phase 3: topics, each belonging to exactly one subject

CREATE TABLE topics (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id     UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    name           VARCHAR(150) NOT NULL,
    description    VARCHAR(500),
    display_order  INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    deleted_at     TIMESTAMPTZ,
    CONSTRAINT uq_topics_subject_name UNIQUE (subject_id, name)
);

CREATE INDEX idx_topics_subject ON topics (subject_id);
