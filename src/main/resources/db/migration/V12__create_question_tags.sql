-- Phase 4: question tags (simple free-text tags, e.g. "percentage", "pnc")

CREATE TABLE question_tags (
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    tag         VARCHAR(50) NOT NULL,
    PRIMARY KEY (question_id, tag)
);

CREATE INDEX idx_question_tags_tag ON question_tags (tag);
