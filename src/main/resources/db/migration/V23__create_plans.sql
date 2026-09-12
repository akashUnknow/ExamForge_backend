-- Phase 9: subscription plans

CREATE TABLE plans (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(150) NOT NULL,
    plan_type      VARCHAR(20)  NOT NULL,
    price          NUMERIC(10,2),
    duration_days  INTEGER,
    description    VARCHAR(1000),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    deleted_at     TIMESTAMPTZ,
    CONSTRAINT uq_plans_name UNIQUE (name)
);
