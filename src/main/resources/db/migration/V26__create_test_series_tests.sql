-- Phase 9: which tests belong to a test series, with display ordering

CREATE TABLE test_series_tests (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_series_id UUID NOT NULL REFERENCES test_series(id) ON DELETE CASCADE,
    test_id        UUID NOT NULL REFERENCES tests(id) ON DELETE RESTRICT,
    display_order  INTEGER NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_test_series_tests_series_test UNIQUE (test_series_id, test_id)
);

CREATE INDEX idx_test_series_tests_series ON test_series_tests (test_series_id);
