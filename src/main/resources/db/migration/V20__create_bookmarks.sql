-- Phase 8: bookmarks. A user saving a question for later - pure add/remove,
-- no soft delete, no updated_at (a bookmark is never edited, only created
-- or removed).

CREATE TABLE bookmarks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_bookmarks_user_question UNIQUE (user_id, question_id)
);

CREATE INDEX idx_bookmarks_user ON bookmarks (user_id);
