-- Phase 3: exam categories (e.g. "Banking", "SSC", "Railways")

CREATE TABLE exam_categories (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(150) NOT NULL,
    description  VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    deleted_at   TIMESTAMPTZ,
    CONSTRAINT uq_exam_categories_name UNIQUE (name)
);
