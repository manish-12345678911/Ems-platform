-- V1__init_incident_schema.sql
-- Incident service schema

CREATE SCHEMA IF NOT EXISTS incident;

-- Extensions must be in public schema so types are available across schemas
SET search_path TO public;
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

SET search_path TO incident, public;

CREATE TABLE incident (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    location GEOGRAPHY(Point, 4326) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    need VARCHAR(16) NOT NULL,
    requires_als BOOLEAN NOT NULL DEFAULT false,
    status VARCHAR(16) NOT NULL DEFAULT 'RECEIVED',
    assigned_unit_id UUID,
    destination_hospital_id UUID,
    dispatched_at TIMESTAMPTZ,
    arrived_scene_at TIMESTAMPTZ,
    arrived_hospital_at TIMESTAMPTZ,
    handed_over_at TIMESTAMPTZ,
    caller_hash CHAR(64),
    CONSTRAINT chk_incident_severity CHECK (severity IN ('CRITICAL','EMERGENCY','URGENT','LOW')),
    CONSTRAINT chk_incident_status CHECK (status IN ('RECEIVED','TRIAGED','DISPATCHED','ON_SCENE','TRANSPORTING','HANDED_OVER','CLOSED','CANCELLED'))
);

CREATE INDEX idx_incident_status ON incident (status);
CREATE INDEX idx_incident_received ON incident (received_at);

-- Outbox event table for transactional outbox pattern
CREATE TABLE outbox_event (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_id UUID NOT NULL,
    topic VARCHAR(64) NOT NULL,
    event_key VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpub ON outbox_event (created_at) WHERE published_at IS NULL;

-- Idempotent consumer dedupe table
CREATE TABLE processed_event (
    consumer VARCHAR(64) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, event_id)
);
