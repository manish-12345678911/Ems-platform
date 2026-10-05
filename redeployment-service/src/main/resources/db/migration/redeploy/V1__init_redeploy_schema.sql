-- V1__init_redeploy_schema.sql
-- Redeployment service schema

CREATE SCHEMA IF NOT EXISTS redeploy;

SET search_path TO public;
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

SET search_path TO redeploy, public;

CREATE TABLE zone (
    id SERIAL PRIMARY KEY,
    centroid GEOGRAPHY(Point, 4326) NOT NULL,
    demand_per_hour DOUBLE PRECISION NOT NULL DEFAULT 0.0
);

CREATE TABLE zone_travel (
    from_zone INT NOT NULL REFERENCES zone(id),
    to_zone INT NOT NULL REFERENCES zone(id),
    travel_sec DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (from_zone, to_zone)
);

CREATE TABLE redeploy_move (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    unit_id UUID NOT NULL,
    target VARCHAR(256) NOT NULL,
    coverage_gain DOUBLE PRECISION NOT NULL,
    accepted BOOLEAN NOT NULL DEFAULT false,
    at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_redeploy_unit ON redeploy_move (unit_id, at DESC);

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
