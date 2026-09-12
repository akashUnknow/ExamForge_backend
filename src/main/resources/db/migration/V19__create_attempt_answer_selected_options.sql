-- Phase 6: which option(s) a user selected for an answer.
--
-- The spec's attempt_answers table describes a singular "selectedOption",
-- which fits MCQ/TRUE_FALSE. Since the question bank also supports
-- MULTIPLE_CHOICE (multiple correct options), this is generalized to a
-- set via a join table rather than a single nullable FK column - MCQ and
-- TRUE_FALSE simply end up with exactly one row here.

CREATE TABLE attempt_answer_selected_options (
    attempt_answer_id UUID NOT NULL REFERENCES attempt_answers(id) ON DELETE CASCADE,
    option_id         UUID NOT NULL REFERENCES question_options(id) ON DELETE CASCADE,
    PRIMARY KEY (attempt_answer_id, option_id)
);
