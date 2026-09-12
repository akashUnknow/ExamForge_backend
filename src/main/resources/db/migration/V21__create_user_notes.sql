-- Phase 8: personal notes a user attaches to a question. One note per
-- user per question (PUT semantics update it in place).

CREATE TABLE user_notes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    note_text   TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_notes_user_question UNIQUE (user_id, question_id)
);

CREATE INDEX idx_user_notes_user ON user_notes (user_id);
