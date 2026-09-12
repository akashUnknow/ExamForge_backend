-- Phase 6: attempt answers

CREATE TABLE attempt_answers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id          UUID NOT NULL REFERENCES test_attempts(id) ON DELETE CASCADE,
    question_id         UUID NOT NULL REFERENCES questions(id) ON DELETE RESTRICT,
    numeric_answer      NUMERIC(12,4),
    marked_for_review   BOOLEAN NOT NULL DEFAULT FALSE,
    answered_at         TIMESTAMPTZ,
    time_spent_seconds  INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_attempt_answers_attempt_question UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_attempt_answers_attempt ON attempt_answers (attempt_id);
