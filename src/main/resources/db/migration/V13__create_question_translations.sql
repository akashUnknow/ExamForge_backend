-- Phase 4: question translations (alternate-language variants of a question)

CREATE TABLE question_translations (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id    UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    language       VARCHAR(10) NOT NULL,
    question_text  TEXT        NOT NULL,
    explanation    TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_question_translations_question_language UNIQUE (question_id, language)
);

CREATE INDEX idx_question_translations_question ON question_translations (question_id);
