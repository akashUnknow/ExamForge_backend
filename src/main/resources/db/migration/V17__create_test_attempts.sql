-- Phase 6: test attempts. Permanent audit record of a user taking a test -
-- no soft delete, no created_by/updated_by (the user_id column already
-- identifies the actor, and an attempt is never edited by anyone but the
-- system's own scoring logic).

CREATE TABLE test_attempts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    test_id           UUID NOT NULL REFERENCES tests(id) ON DELETE RESTRICT,
    started_at        TIMESTAMPTZ NOT NULL,
    expires_at        TIMESTAMPTZ NOT NULL,
    submitted_at      TIMESTAMPTZ,
    status            VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    score             NUMERIC(8,2),
    correct_count     INTEGER,
    incorrect_count   INTEGER,
    unanswered_count  INTEGER,
    accuracy          NUMERIC(5,2),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_test_attempts_user   ON test_attempts (user_id);
CREATE INDEX idx_test_attempts_test   ON test_attempts (test_id);
CREATE INDEX idx_test_attempts_status ON test_attempts (status);

-- At most one IN_PROGRESS attempt per user per test - startAttempt()
-- resumes the existing one instead of creating a duplicate, and this
-- constraint is the backstop against a race producing two.
CREATE UNIQUE INDEX uq_test_attempts_active_per_user_test
    ON test_attempts (user_id, test_id)
    WHERE status = 'IN_PROGRESS';
