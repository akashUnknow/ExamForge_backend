-- Phase 5: test questions (which questions belong to a test, with
-- per-test marks overrides and ordering)

CREATE TABLE test_questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id         UUID NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
    question_id     UUID NOT NULL REFERENCES questions(id) ON DELETE RESTRICT,
    section_id      UUID REFERENCES test_sections(id) ON DELETE SET NULL,
    display_order   INTEGER      NOT NULL DEFAULT 0,
    marks           NUMERIC(6,2) NOT NULL,
    negative_marks  NUMERIC(6,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_test_questions_test_question UNIQUE (test_id, question_id)
);

CREATE INDEX idx_test_questions_test    ON test_questions (test_id);
CREATE INDEX idx_test_questions_section ON test_questions (section_id);
