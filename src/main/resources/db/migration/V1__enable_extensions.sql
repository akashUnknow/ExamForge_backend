-- Phase 1: Foundation
-- Enable UUID generation support used by every table's primary key
-- across all future modules (auth, exam, question, test, etc.)

CREATE EXTENSION IF NOT EXISTS pgcrypto;
