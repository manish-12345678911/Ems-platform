# Chapter 6: Database Schemas, Relational DDL & Kafka Event Contracts

## 6.1 Database Architecture & Schema Isolation
The platform implements the **Database-per-Service** design pattern utilizing a shared high-performance PostgreSQL 15 database instance partitioned into isolated logical schemas. Spatial capabilities are enabled via PostGIS 3.4.

```
+-----------------------------------------------------------------------------------------------+
|                             POSTGRESQL 15 DATABASE INSTANCE ('h8')                             |
+-------------------+-------------------+-------------------+-------------------+---------------+
| Schema: incident  | Schema: dispatch  | Schema: hospital  | Schema: audit     | PostGIS 3.4   |
+-------------------+-------------------+-------------------+-------------------+---------------+
| - incidents       | - ambulance_units | - hospitals       | - audit_ledger    | - spatial_ref |
| - outbox_events   | - assignments     | - ed_alerts       | - outbox_events   | - GIST indexes|
| - processed_events| - outbox_events   | - outbox_events   | - processed_events| - geometries  |
|                   | - processed_events| - processed_events|                   |               |
+-------------------+-------------------+-------------------+-------------------+---------------+
```

---

## 6.2 Relational Database DDL (PostgreSQL 15 + PostGIS)

### 6.2.1 Dispatch Service Schema (`dispatch`)
```sql
CREATE SCHEMA IF NOT EXISTS dispatch;

-- Enable PostGIS extension if not present
CREATE EXTENSION IF NOT EXISTS postgis;

-- Fleet Ambulance Units Table
CREATE TABLE dispatch.ambulance_units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    call_sign VARCHAR(32) NOT NULL UNIQUE,
    vehicle_type VARCHAR(16) NOT NULL CHECK (vehicle_type IN ('ALS', 'BLS')),
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE' 
        CHECK (status IN ('AVAILABLE', 'ASSIGNED', 'EN_ROUTE', 'ON_SCENE', 'TRANSPORTING', 'AT_HOSPITAL', 'MAINTENANCE')),
    location GEOMETRY(Point, 4326) NOT NULL,
    shift_missions INT NOT NULL DEFAULT 0,
    active_shift_minutes BIGINT NOT NULL DEFAULT 0,
    last_telemetry_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Spatial GIST Index for high-speed radius candidate queries
CREATE INDEX idx_units_location ON dispatch.ambulance_units USING GIST (location);
CREATE INDEX idx_units_status ON dispatch.ambulance_units (status);

-- Mission Assignments Table
CREATE TABLE dispatch.assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL,
    unit_id UUID NOT NULL REFERENCES dispatch.ambulance_units(id),
    status VARCHAR(32) NOT NULL DEFAULT 'ASSIGNED',
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    on_scene_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_assignments_incident ON dispatch.assignments (incident_id);
CREATE INDEX idx_assignments_unit ON dispatch.assignments (unit_id);

-- Transactional Outbox Table
CREATE TABLE dispatch.outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_pending ON dispatch.outbox_events (status, created_at) WHERE status = 'PENDING';

-- Consumer Idempotency Table
CREATE TABLE dispatch.processed_events (
    event_id UUID PRIMARY KEY,
    source_topic VARCHAR(64) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

### 6.2.2 Incident Service Schema (`incident`)
```sql
CREATE SCHEMA IF NOT EXISTS incident;

CREATE TABLE incident.incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    caller_phone_hash VARCHAR(64) NOT NULL, -- Salted SHA-256 (HIPAA/DISHA compliant)
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    severity VARCHAR(32) NOT NULL CHECK (severity IN ('CRITICAL', 'URGENT', 'STANDARD', 'NON_EMERGENCY')),
    clinical_need VARCHAR(32) NOT NULL CHECK (clinical_need IN ('CARDIAC', 'TRAUMA', 'STROKE', 'PEDIATRIC', 'RESPIRATORY', 'GENERAL')),
    requires_als BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED' 
        CHECK (status IN ('CREATED', 'TRIAGED', 'DISPATCHED', 'ON_SCENE', 'TRANSPORTING', 'RESOLVED', 'CANCELLED')),
    triage_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_incidents_status ON incident.incidents (status);
CREATE INDEX idx_incidents_created ON incident.incidents (created_at DESC);

CREATE TABLE incident.outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE incident.processed_events (
    event_id UUID PRIMARY KEY,
    source_topic VARCHAR(64) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

### 6.2.3 Hospital Service Schema (`hospital`)
```sql
CREATE SCHEMA IF NOT EXISTS hospital;

CREATE TABLE hospital.hospitals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(128) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    capabilities VARCHAR(256) NOT NULL, -- Comma-delimited: 'TRAUMA,CARDIAC,STROKE,PEDIATRIC'
    total_ed_beds INT NOT NULL,
    free_ed_beds INT NOT NULL,
    free_icu_beds INT NOT NULL,
    available_ventilators INT NOT NULL,
    diversion_active BOOLEAN NOT NULL DEFAULT FALSE,
    diversion_reason VARCHAR(256),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE hospital.pre_arrival_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL REFERENCES hospital.hospitals(id),
    unit_id UUID NOT NULL,
    eta_seconds INT NOT NULL,
    clinical_vitals JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

### 6.2.4 Audit Service Schema (`audit`)
```sql
CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE audit.audit_ledger (
    sequence_id BIGSERIAL PRIMARY KEY,
    block_hash VARCHAR(64) NOT NULL UNIQUE,
    previous_hash VARCHAR(64) NOT NULL,
    event_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_event ON audit.audit_ledger (event_id);
```

---

## 6.3 Kafka Topic Architecture & Event Contracts

The platform operates seven core event topics within the Apache Kafka message broker:

```
+-----------------------------------------------------------------------------------------------+
|                                      KAFKA TOPIC TOPOLOGY                                     |
+-------------------------------+-----------------------+-------------------+-------------------+
| Topic Name                    | Partitioning Key      | Retention Policy  | Cleanup Policy    |
+-------------------------------+-----------------------+-------------------+-------------------+
| ems.incident.created          | incidentId            | 7 Days            | delete            |
| ems.unit.dispatched           | unitId                | 7 Days            | delete            |
| ems.unit.location.updated     | unitId                | 2 Hours           | delete            |
| ems.unit.status.changed       | unitId                | 7 Days            | delete            |
| ems.hospital.capacity.updated | hospitalId            | 3 Days            | compact           |
| ems.hospital.divert.toggled   | hospitalId            | 30 Days           | compact           |
| ems.audit.event               | sequenceId            | 365 Days          | delete (Archive)  |
+-------------------------------+-----------------------+-------------------+-------------------+
```

### 6.3.1 Contract: `IncidentCreatedEvent`
* **Topic**: `ems.incident.created`
* **Key**: `incidentId` (UUID string)
```json
{
  "eventId": "e4b3c2a1-5555-4444-3333-222211110000",
  "eventType": "IncidentCreatedEvent",
  "timestamp": "2026-10-05T09:30:15.124Z",
  "incidentId": "00000000-0000-0000-0000-000000000101",
  "location": {
    "latitude": 26.9124,
    "longitude": 75.7873
  },
  "severity": "CRITICAL",
  "clinicalNeed": "CARDIAC",
  "requiresAls": true,
  "callerPhoneHash": "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
}
```

### 6.3.2 Contract: `UnitDispatchedEvent`
* **Topic**: `ems.unit.dispatched`
* **Key**: `unitId` (UUID string)
```json
{
  "eventId": "f1a2b3c4-1111-2222-3333-444455556666",
  "eventType": "UnitDispatchedEvent",
  "timestamp": "2026-10-05T09:30:18.450Z",
  "incidentId": "00000000-0000-0000-0000-000000000101",
  "unitId": "cccccccc-3333-3333-3333-333333333333",
  "callSign": "AMB-03",
  "unitType": "ALS",
  "dispatchScore": 0.942,
  "estimatedEtaSeconds": 180,
  "isOverride": false,
  "overrideReason": null
}
```

### 6.3.3 Contract: `UnitLocationUpdatedEvent`
* **Topic**: `ems.unit.location.updated`
* **Key**: `unitId` (UUID string)
```json
{
  "eventId": "d9c8b7a6-9999-8888-7777-666655554444",
  "eventType": "UnitLocationUpdatedEvent",
  "timestamp": "2026-10-05T09:31:00.000Z",
  "unitId": "cccccccc-3333-3333-3333-333333333333",
  "callSign": "AMB-03",
  "latitude": 26.9012,
  "longitude": 75.8055,
  "speedKmH": 48.5,
  "bearingDegrees": 142.0,
  "status": "EN_ROUTE"
}
```

### 6.3.4 Contract: `PreArrivalAlertEvent`
* **Topic**: `ems.hospital.prearrival`
* **Key**: `hospitalId` (UUID string)
```json
{
  "eventId": "b5a4c3d2-7777-6666-5555-444433332222",
  "eventType": "PreArrivalAlertEvent",
  "timestamp": "2026-10-05T09:42:30.120Z",
  "hospitalId": "aaaaaaaa-0001-0001-0001-000000000001",
  "unitId": "cccccccc-3333-3333-3333-333333333333",
  "etaSeconds": 340,
  "clinicalVitals": {
    "bloodPressureSystolic": 110,
    "bloodPressureDiastolic": 70,
    "heartRateBpm": 92,
    "respiratoryRate": 18,
    "spO2Percentage": 97,
    "glasgowComaScale": 14,
    "traumaScore": 12,
    "cardiacRhythm": "SINUS_TACHYCARDIA"
  }
}
```

---

## 6.4 Redis Ephemeral Data Structures

The `tracking-service` and `dispatch-service` utilize Redis 7 for high-speed in-memory indexing:

1. **Geospatial Fleet Index**:
   * **Key**: `units:geo`
   * **Data Structure**: Redis Geospatial (Sorted Set under the hood)
   * **Command**: `GEOADD units:geo 75.8164 26.8988 cccccccc-3333-3333-3333-333333333333`
   * **Radius Query**: `GEORADIUS units:geo 75.7873 26.9124 25 km WITHCOORD WITHDIST`
2. **Telemetry Hash with TTL**:
   * **Key**: `units:telemetry:{unitId}`
   * **Fields**: `lat`, `lon`, `speed`, `bearing`, `lastSeenEpoch`, `status`
   * **TTL**: 60 seconds (Auto-eviction enforces staleness protocol)
3. **Distributed Locks (Redlock)**:
   * **Key**: `lock:unit:{unitId}`
   * **TTL**: 5000 milliseconds (Protects concurrent resource operations)
