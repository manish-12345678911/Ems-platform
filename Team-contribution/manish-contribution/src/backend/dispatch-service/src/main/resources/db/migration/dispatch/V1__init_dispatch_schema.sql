-- V1__init_dispatch_schema.sql
-- Dispatch service schema

CREATE SCHEMA IF NOT EXISTS dispatch;

SET search_path TO public;
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

SET search_path TO dispatch, public;

CREATE TABLE station (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(128) NOT NULL,
    location GEOGRAPHY(Point, 4326) NOT NULL
);

CREATE TABLE ambulance_unit (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    call_sign VARCHAR(16) NOT NULL UNIQUE,
    type VARCHAR(8) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
    position GEOGRAPHY(Point, 4326),
    position_at TIMESTAMPTZ,
    shift_start TIMESTAMPTZ,
    home_station_id UUID REFERENCES station(id),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_unit_type CHECK (type IN ('ALS','BLS')),
    CONSTRAINT chk_unit_status CHECK (status IN ('AVAILABLE','DISPATCHED','ON_SCENE','TRANSPORTING','AT_HOSPITAL','OFFLINE'))
);

CREATE INDEX idx_unit_status ON ambulance_unit (status);

CREATE TABLE assignment (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    incident_id UUID NOT NULL,
    unit_id UUID NOT NULL REFERENCES ambulance_unit(id),
    ranked_snapshot JSONB,
    chosen_by VARCHAR(64),
    decided_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    rejected BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_assignment_incident ON assignment (incident_id);

-- Outbox and dedupe tables
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

CREATE TABLE processed_event (
    consumer VARCHAR(64) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, event_id)
);
