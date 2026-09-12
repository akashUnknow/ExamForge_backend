-- Phase 4: question options (for MCQ / MULTIPLE_CHOICE / TRUE_FALSE types)

CREATE TABLE question_options (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id    UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    option_text    VARCHAR(1000) NOT NULL,
    is_correct     BOOLEAN       NOT NULL DEFAULT FALSE,
    display_order  INTEGER       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_question_options_question ON question_options (question_id);
