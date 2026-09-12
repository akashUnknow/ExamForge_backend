-- Phase 4: questions

CREATE TABLE questions (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id               UUID NOT NULL REFERENCES topics(id) ON DELETE RESTRICT,
    question_type          VARCHAR(30)  NOT NULL,
    question_text          TEXT         NOT NULL,
    explanation            TEXT,
    difficulty             VARCHAR(20)  NOT NULL,
    marks                  NUMERIC(6,2) NOT NULL,
    negative_marks         NUMERIC(6,2) NOT NULL DEFAULT 0,
    language                VARCHAR(10) NOT NULL DEFAULT 'en',
    status                 VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    rejection_reason       VARCHAR(1000),
    correct_numeric_answer NUMERIC(12,4),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100),
    deleted_at             TIMESTAMPTZ
);

CREATE INDEX idx_questions_topic       ON questions (topic_id);
CREATE INDEX idx_questions_status      ON questions (status) WHERE deleted_at IS NULL;
CREATE INDEX idx_questions_difficulty  ON questions (difficulty);
CREATE INDEX idx_questions_type        ON questions (question_type);
