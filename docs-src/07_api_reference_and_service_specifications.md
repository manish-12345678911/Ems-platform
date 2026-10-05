# Chapter 7: API Reference & Microservice Endpoints

## 7.1 Global API Standards & Conventions
* **Protocols**: HTTP/1.1 and HTTP/2 over TLS 1.3.
* **Payload Encoding**: UTF-8 encoded `application/json`.
* **Authentication**: Bearer JWT tokens in the `Authorization` header (`Authorization: Bearer eyJhbGci...`).
* **Timestamp Standards**: ISO-8601 UTC with millisecond precision (`YYYY-MM-DDTHH:mm:ss.sssZ`).
* **Coordinates Standard**: WGS-84 (`latitude`: -90.0 to +90.0, `longitude`: -180.0 to +180.0).

---

## 7.2 Incident Service API (`incident-service` / :8081)

### `POST /incidents`
Creates and triages an incoming emergency incident.
* **Headers**: `Content-Type: application/json`
* **Request Body**:
```json
{
  "callerPhone": "+919876543210",
  "latitude": 26.9124,
  "longitude": 75.7873,
  "severity": "CRITICAL",
  "clinicalNeed": "CARDIAC",
  "requiresAls": true,
  "triageNotes": "Male, 54, acute retrosternal chest pain radiating to left arm, diaphoretic."
}
```
* **Response (201 Created)**:
```json
{
  "incidentId": "00000000-0000-0000-0000-000000000101",
  "status": "CREATED",
  "phoneHash": "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
  "severity": "CRITICAL",
  "clinicalNeed": "CARDIAC",
  "requiresAls": true,
  "createdAt": "2026-10-05T09:30:15.124Z"
}
```

### `GET /incidents/{id}`
Retrieves current operational status and lifecycle history of an incident.
* **Response (200 OK)**:
```json
{
  "incidentId": "00000000-0000-0000-0000-000000000101",
  "status": "DISPATCHED",
  "assignedUnitId": "cccccccc-3333-3333-3333-333333333333",
  "callSign": "AMB-03",
  "latitude": 26.9124,
  "longitude": 75.7873,
  "severity": "CRITICAL",
  "createdAt": "2026-10-05T09:30:15.124Z",
  "updatedAt": "2026-10-05T09:30:18.450Z"
}
```

---

## 7.3 Dispatch Service API (`dispatch-service` / :8082)

### `GET /dispatch/candidates`
Executes `DispatchScorer` multi-factor scoring against all available ambulances.
* **Query Parameters**:
  * `lat` (double, required): Incident latitude.
  * `lon` (double, required): Incident longitude.
  * `severity` (string, required): `CRITICAL`, `URGENT`, `STANDARD`, or `NON_EMERGENCY`.
  * `need` (string, required): `CARDIAC`, `TRAUMA`, `STROKE`, `PEDIATRIC`, etc.
  * `requiresAls` (boolean, optional, default: false): True if ALS equipment/crew is required.
* **Response (200 OK)**:
```json
[
  {
    "unitId": "cccccccc-3333-3333-3333-333333333333",
    "callSign": "AMB-03",
    "type": "ALS",
    "score": 0.21002,
    "distanceKm": 3.257,
    "etaSeconds": 180.4,
    "etaComponent": 0.0250,
    "capabilityComponent": 0.0,
    "fatigueComponent": 1.0,
    "coverageComponent": 0.0,
    "stalenessComponent": 1.0
  },
  {
    "unitId": "99999999-0009-0009-0009-000000000009",
    "callSign": "AMB-09",
    "type": "ALS",
    "score": 0.21288,
    "distanceKm": 4.188,
    "etaSeconds": 231.9,
    "etaComponent": 0.0322,
    "capabilityComponent": 0.0,
    "fatigueComponent": 1.0,
    "coverageComponent": 0.0,
    "stalenessComponent": 1.0
  },
  {
    "unitId": "dddddddd-4444-4444-4444-444444444444",
    "callSign": "AMB-04",
    "type": "BLS",
    "score": 0.45235,
    "distanceKm": 0.766,
    "etaSeconds": 42.4,
    "etaComponent": 0.0058,
    "capabilityComponent": 1.0,
    "fatigueComponent": 1.0,
    "coverageComponent": 0.0,
    "stalenessComponent": 1.0
  }
]
```

### `POST /dispatch`
Executes atomic unit reservation and creates dispatch assignment.
* **Request Body**:
```json
{
  "incidentId": "00000000-0000-0000-0000-000000000101",
  "unitId": "cccccccc-3333-3333-3333-333333333333"
}
```
* **Response (200 OK)**:
```json
{
  "assignmentId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "incidentId": "00000000-0000-0000-0000-000000000101",
  "unitId": "cccccccc-3333-3333-3333-333333333333",
  "callSign": "AMB-03",
  "status": "ASSIGNED",
  "assignedAt": "2026-10-05T09:30:18.450Z"
}
```
* **Error Response (409 Conflict)**:
```json
{
  "error": "UNIT_ALREADY_COMMITTED",
  "message": "Unit AMB-03 was concurrently reserved by another operator.",
  "unitStatus": "ASSIGNED"
}
```

### `POST /dispatch/override`
Executes manual supervisor override with mandatory legal justification.
* **Request Body**:
```json
{
  "incidentId": "00000000-0000-0000-0000-000000000101",
  "recommendedUnitId": "cccccccc-3333-3333-3333-333333333333",
  "selectedUnitId": "aaaaaaaa-1111-1111-1111-111111111111",
  "overrideReason": "AMB-03 reported mechanical siren failure during call dispatch."
}
```
* **Response (200 OK)**: Emits `OverrideAssignedResponse` and records audit entry.

---

## 7.4 Tracking Service API (`tracking-service` / :8083)

### `POST /tracking/location`
Ingests high-frequency GPS telemetry from mobile PWA or IoT device.
* **Request Body**:
```json
{
  "unitId": "cccccccc-3333-3333-3333-333333333333",
  "latitude": 26.9012,
  "longitude": 75.8055,
  "speedKmH": 52.4,
  "bearingDegrees": 138.5,
  "timestamp": "2026-10-05T09:31:00.000Z"
}
```
* **Response (200 OK)**:
```json
{
  "status": "ACCEPTED",
  "unitId": "cccccccc-3333-3333-3333-333333333333",
  "ttlSeconds": 60
}
```

### `GET /tracking/units`
Returns active locations for all registered units in the fleet.
* **Response (200 OK)**:
```json
[
  {
    "unitId": "cccccccc-3333-3333-3333-333333333333",
    "callSign": "AMB-03",
    "latitude": 26.9012,
    "longitude": 75.8055,
    "speedKmH": 52.4,
    "bearingDegrees": 138.5,
    "isStale": false,
    "secondsSinceLastFix": 1.4
  }
]
```

---

## 7.5 Routing Service API (`routing-service` / :8084)

### `POST /routes/directions`
Calculates turn-by-turn road route and driving ETA between two coordinates.
* **Request Body**:
```json
{
  "start": { "latitude": 26.8988, "longitude": 75.8164 },
  "end": { "latitude": 26.9124, "longitude": 75.7873 }
}
```
* **Response (200 OK)**:
```json
{
  "distanceMeters": 4250.0,
  "durationSeconds": 312.0,
  "geometry": "w`~mDqq_eM~... (Polyline encoded string)",
  "source": "GRAPHHOPPER_OSM"
}
```

### `POST /routes/matrix`
Computes an $N \times 1$ one-to-many travel time matrix from multiple ambulance positions to an incident.

---

## 7.6 Hospital Service API (`hospital-service` / :8085)

### `GET /hospitals`
Returns complete list of receiving emergency centers with current capabilities and live bed headroom.
* **Response (200 OK)**:
```json
[
  {
    "id": "00000000-0000-0000-0000-000000000010",
    "name": "SMS Medical College & Hospital (Apex Trauma)",
    "capabilities": ["STROKE", "CARDIAC", "TRAUMA"],
    "totalEdBeds": 120,
    "freeEdBeds": 18,
    "freeIcuBeds": 4,
    "diversionActive": false,
    "latitude": 26.8988,
    "longitude": 75.8164
  }
]
```

### `POST /hospitals/handover`
Transmits pre-arrival clinical data to the receiving facility.
* **Request Body**:
```json
{
  "hospitalId": "00000000-0000-0000-0000-000000000010",
  "unitId": "cccccccc-3333-3333-3333-333333333333",
  "etaSeconds": 340,
  "vitals": {
    "bloodPressure": "118/76",
    "heartRate": 88,
    "spO2": 98,
    "gcs": 15
  }
}
```

### `GET /hospitals/alerts/stream`
Server-Sent Events (SSE) endpoint providing streaming real-time pre-arrival cards to the hospital triage team.

---

## 7.7 Audit Service API (`audit-service` / :8087)

### `GET /audit/verify`
Recalculates and cryptographically verifies the SHA-256 hash chain of the entire dispatch ledger.
* **Response (200 OK)**:
```json
{
  "valid": true,
  "totalEntries": 4829,
  "genesisHash": "0000000000000000000000000000000000000000000000000000000000000000",
  "latestBlockHash": "8f3b2c1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b",
  "verifiedAt": "2026-10-05T09:35:00.000Z",
  "details": "All block signatures mathematically verified. Zero tampering detected."
}
```
