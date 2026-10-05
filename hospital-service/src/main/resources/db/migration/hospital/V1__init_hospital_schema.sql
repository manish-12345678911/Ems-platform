-- V1__init_hospital_schema.sql
-- Hospital service schema

CREATE SCHEMA IF NOT EXISTS hospital;

SET search_path TO public;
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

SET search_path TO hospital, public;

CREATE TABLE hospital (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(256) NOT NULL,
    location GEOGRAPHY(Point, 4326) NOT NULL
);

CREATE TABLE hospital_capability (
    hospital_id UUID NOT NULL REFERENCES hospital(id),
    need VARCHAR(16) NOT NULL,
    PRIMARY KEY (hospital_id, need)
);

CREATE TABLE capacity_snapshot (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hospital_id UUID NOT NULL REFERENCES hospital(id),
    ed_beds_free INT NOT NULL DEFAULT 0,
    icu_beds_free INT NOT NULL DEFAULT 0,
    ventilators_free INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_capacity_hospital ON capacity_snapshot (hospital_id, updated_at DESC);

CREATE TABLE pre_arrival_alert (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    incident_id UUID NOT NULL,
    hospital_id UUID NOT NULL REFERENCES hospital(id),
    eta_seconds INT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    acked_at TIMESTAMPTZ
);

CREATE INDEX idx_alert_hospital ON pre_arrival_alert (hospital_id, sent_at DESC);

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
