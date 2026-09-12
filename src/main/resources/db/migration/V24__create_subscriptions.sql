-- Phase 9: subscriptions. A permanent history of a user's subscription
-- states over time - like test_attempts, not soft-deleted; cancelling
-- is a status change, not a row deletion.

CREATE TABLE subscriptions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    plan_id     UUID NOT NULL REFERENCES plans(id) ON DELETE RESTRICT,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    start_date  TIMESTAMPTZ NOT NULL,
    end_date    TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_subscriptions_user   ON subscriptions (user_id);
CREATE INDEX idx_subscriptions_status ON subscriptions (status);

-- At most one ACTIVE subscription per user at a time.
CREATE UNIQUE INDEX uq_subscriptions_active_per_user
    ON subscriptions (user_id)
    WHERE status = 'ACTIVE';
