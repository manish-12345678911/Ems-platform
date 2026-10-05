-- V1__init_audit_schema.sql
-- Audit service schema

CREATE SCHEMA IF NOT EXISTS audit;

SET search_path TO public;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

SET search_path TO audit, public;

CREATE TABLE audit_log (
    seq BIGSERIAL PRIMARY KEY,
    event_id UUID UNIQUE NOT NULL,
    kind VARCHAR(32) NOT NULL,
    actor VARCHAR(64),
    payload JSONB NOT NULL,
    prev_hash CHAR(64),
    hash CHAR(64) NOT NULL,
    at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Hash chain: hash = SHA-256(prev_hash || payload || at)
-- Append-only: revoke UPDATE/DELETE for the service role
COMMENT ON TABLE audit_log IS 'Append-only audit log with SHA-256 hash chain for tamper detection';

-- Idempotent consumer dedupe table
CREATE TABLE processed_event (
    consumer VARCHAR(64) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, event_id)
);
