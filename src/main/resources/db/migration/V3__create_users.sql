-- Phase 2: users table

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(150) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    mobile          VARCHAR(20)  NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uq_users_email  UNIQUE (email),
    CONSTRAINT uq_users_mobile UNIQUE (mobile)
);

CREATE INDEX idx_users_email  ON users (email);
CREATE INDEX idx_users_mobile ON users (mobile);
CREATE INDEX idx_users_active ON users (active) WHERE deleted_at IS NULL;
