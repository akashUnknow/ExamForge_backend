-- Phase 8: study material metadata. The actual file bytes are never
-- stored here - only a storage_key referencing wherever FileStorageService
-- put the file (local disk in this phase; an S3-compatible backend can be
-- swapped in later against the same interface without a migration).

CREATE TABLE study_materials (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title              VARCHAR(200) NOT NULL,
    description        VARCHAR(2000),
    material_type      VARCHAR(30)  NOT NULL,
    exam_id            UUID REFERENCES exams(id) ON DELETE SET NULL,
    subject_id         UUID REFERENCES subjects(id) ON DELETE SET NULL,
    storage_key        VARCHAR(500) NOT NULL,
    original_filename  VARCHAR(255) NOT NULL,
    content_type       VARCHAR(150) NOT NULL,
    file_size_bytes    BIGINT       NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    deleted_at         TIMESTAMPTZ
);

CREATE INDEX idx_study_materials_exam    ON study_materials (exam_id);
CREATE INDEX idx_study_materials_subject ON study_materials (subject_id);
CREATE INDEX idx_study_materials_type    ON study_materials (material_type);
