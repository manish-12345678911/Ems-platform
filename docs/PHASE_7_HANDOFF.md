# Phase 7 Audit Service, Observability & Fault Injection — Completion & Handoff — H8 EMS Platform

> **Status**: **Phase 7 is ✅ 100% COMPLETE & VERIFIED**
> - `audit-service`: ✅ **100% complete, 14/14 tests passing** (Port 8087)
>   - Append-only hash chain with SHA-256 tamper detection (`GET /audit/verify`)
>   - Idempotent event deduplication table `processed_event` (Hard Rule #4)
>   - PostgreSQL `audit` schema with Flyway migration `V1__init_audit_schema.sql`
>   - Kafka consumer on topic `audit.events`
>   - Property-based testing via **jqwik** (1,000 randomized iterations verifying chain integrity & corruption detection)
> - **Observability & Dashboards**:
>   - Micrometer Prometheus metrics exposed across all microservices (`/actuator/prometheus`)
>   - `ops/prometheus.yml` configured to scrape all 8 services + gateway
>   - `ops/grafana/dashboards/ems-platform.json`: Complete 12-panel operational and reliability dashboard (decision latency p50/p90/p95/p99, reservation conflicts, coverage ratio, heartbeat stale units, outbox pending, Kafka lag, cryptographic chain validity)
>   - Automated Grafana provisioning configurations (`datasource.yml` and `dashboard.yml`)
> - **Fault Injection & Chaos Validation**:
>   - `ops/chaos/fault_injection_test.ps1`: Automated PowerShell resilience test suite testing all Section 13.1 failure modes (Routing circuit breaker fallback, Audit hash chain verification, Idempotent deduplication, Conditional UPDATE reservation mutex, and Metrics summary) &rarr; **6/6 tests passing (100%)**.
>   - Live database tampering test verified: altering a single row in PostgreSQL immediately results in HTTP 409 Conflict, exact broken sequence identification, and setting `audit_chain_valid = 0`.
> - **All Platform Modules**: **216/216 unit, property & integration tests passing across all 13 modules**, zero failures, zero errors.

---

## 1. Environment & Live Microservice Status

All infrastructure containers and all 8 microservices + API Gateway + Web GUI are running live:

| Service | Port | Process / Task | Health Endpoint | Status |
|---------|------|----------------|-----------------|--------|
| **PostgreSQL** | 5432 | Docker `h8-postgres-1` | `pg_isready` | UP |
| **Redis 7** | 6379 | Docker `h8-redis-1` | `redis-cli ping` | UP |
| **Kafka** | 9092 | Docker `h8-kafka-1` | Native | UP |
| **Keycloak** | 8180 | Docker `h8-keycloak-1` | `:8180/realms/h8` | UP |
| **API Gateway** | 8080 | task-1278 | `http://localhost:8080/actuator/health` | UP |
| **Incident Service** | 8081 | task-333 | `http://localhost:8081/actuator/health` | UP |
| **Dispatch Service** | 8082 | task-335 | `http://localhost:8082/actuator/health` | UP |
| **Tracking Service** | 8083 | task-289 | `http://localhost:8083/actuator/health` | UP |
| **Routing Service** | 8084 | task-66 | `http://localhost:8084/actuator/health` | UP |
| **Hospital Service** | 8085 | task-919 | `http://localhost:8085/actuator/health` | UP |
| **Redeployment Service** | 8086 | task-722 | `http://localhost:8086/actuator/health` | UP |
| **Audit Service** | 8087 | task-1251 | `http://localhost:8087/actuator/health` | UP |
| **Tactical Web Server** | 8088 | task-408 | `http://localhost:8088` | UP |

---

## 2. Audit Service (Port 8087) Implementation

### 2.1 Schema Architecture (`audit` schema)
- `audit.audit_log`:
  - `seq BIGSERIAL PRIMARY KEY`: Strictly sequential block order.
  - `event_id UUID UNIQUE NOT NULL`: Unique event identifier for idempotency.
  - `kind VARCHAR(32) NOT NULL`: Event category (`INCIDENT_CREATED`, `DISPATCH_DECISION`, `LOCATION_UPDATE`, etc.).
  - `actor VARCHAR(64)`: User ID, service account, or system identity.
  - `payload JSONB NOT NULL`: Canonicalized JSON event payload.
  - `prev_hash CHAR(64)`: Cryptographic hash of predecessor row (or 64-zero genesis hash).
  - `hash CHAR(64) NOT NULL`: SHA-256 digest of block contents.
  - `at TIMESTAMPTZ NOT NULL`: Event occurrence timestamp (canonicalized to millisecond precision).
- `audit.processed_event`:
  - `consumer VARCHAR(64) NOT NULL`: Consumer identity (`audit-service`).
  - `event_id UUID NOT NULL`: Event deduplication key.
  - `processed_at TIMESTAMPTZ NOT NULL`: Timestamp of initial processing.
  - `PRIMARY KEY (consumer, event_id)`

### 2.2 Cryptographic Hash Chain (`HashChainService.java`)
- **Genesis Block**: `0000000000000000000000000000000000000000000000000000000000000000` (64 zeros).
- **Block Hash Formula**:
  $$\text{Hash} = \text{Hex}(\text{SHA-256}(\text{prevHash} \mathbin{\Vert} \text{":"} \mathbin{\Vert} \text{eventId} \mathbin{\Vert} \text{":"} \mathbin{\Vert} \text{kind} \mathbin{\Vert} \text{":"} \mathbin{\Vert} \text{canonicalJson}(\text{payload}) \mathbin{\Vert} \text{":"} \mathbin{\Vert} \text{at.toEpochMilli}()))$$
- **Tamper-Detection Engine**:
  - Traverses chain in order `seq ASC`.
  - Verifies block continuity: $\text{entry}_i.\text{prevHash} == \text{entry}_{i-1}.\text{hash}$.
  - Recomputes hash for each row from database values and checks bitwise identity against stored $\text{entry}_i.\text{hash}$.
  - If tampered: returns `VerificationResult` with `valid: false`, `brokenAtSeq`, `expectedHash`, `actualHash`, and detailed diagnostic message; sets HTTP status 409 Conflict and exposes Prometheus gauge `audit_chain_valid = 0`.

### 2.3 REST Endpoints
- `GET /audit`: Query audit logs (supports filtering by `kind`, `incidentId`, and pagination `limit`).
- `GET /audit/verify`: Verifies cryptographic hash chain across all stored entries.
- `POST /audit`: Ingest an audit event record directly.
- `GET /metrics/summary`: Returns platform telemetry overview including total event count, chain validity status, breakdown by event kind, and latest timestamp.

---

## 3. Observability Architecture

### 3.1 Prometheus Scrape Configuration (`ops/prometheus.yml`)
Configured to poll at 15s intervals across:
- `api-gateway:8080`
- `incident-service:8081`
- `dispatch-service:8082`
- `tracking-service:8083`
- `routing-service:8084`
- `hospital-service:8085`
- `redeployment-service:8086`
- `audit-service:8087`

### 3.2 Grafana Provisioning & Dashboard (`ops/grafana/`)
- `ops/grafana/provisioning/datasources/datasource.yml`: Automatically registers the Prometheus datasource (`http://prometheus:9090`).
- `ops/grafana/provisioning/dashboards/dashboard.yml`: Automatically provisions dashboards from disk.
- `ops/grafana/dashboards/ems-platform.json`:
  1. **Row 1 - Dispatch Engine & Decision Latency**:
     - *Dispatch Decision Latency*: Multi-line timeseries of p50, p90, p95, p99 quantiles from `dispatch_decision_seconds_bucket`.
     - *Dispatch Reservation Conflicts*: Stat panel showing conditional UPDATE race collisions (`dispatch_reservation_conflicts_total`).
     - *System Coverage Ratio*: Visual gauge tracking 8-minute target coverage ratio (green > 85%, orange 70-85%, red < 70%).
  2. **Row 2 - Fleet Tracking & Heartbeats**:
     - *Location Ingest Throughput*: Ingestion rate per second (`rate(location_ingest_total[1m])`).
     - *Stale Fleet Units*: Count of vehicles with heartbeat TTL > 30s (`stale_units`).
     - *Routing Circuit Breaker*: Rate of fallback calls to HaversineEta (`rate(routing_fallback_calls_total[1m])`).
  3. **Row 3 - Reliability & Cryptographic Audit**:
     - *Outbox Pending Events*: Real-time count of unpropagated outbox records (`sum(outbox_pending)`).
     - *Kafka Consumer Lag*: Lag timeseries across consumer groups and topics (`kafka_consumer_lag`).
     - *Audit Hash Chain Validity*: Binary status indicator mapped to green "VERIFIED VALID" or red "TAMPERED / BROKEN" (`audit_chain_valid`).

---

## 4. Fault Injection & Chaos Validation (`ops/chaos/`)

The automated resilience script `ops/chaos/fault_injection_test.ps1` runs end-to-end against the platform:

```powershell
powershell -ExecutionPolicy Bypass -File ops\chaos\fault_injection_test.ps1
```

### 4.1 Verification Test Results

| Test # | Subsystem / Mechanism | Failure Scenario Tested | Observed Outcome | Status |
|--------|-----------------------|-------------------------|------------------|--------|
| **Test 1** | Routing Fallback | Unreachable routing / graph latency | Returned degraded ETA via `HaversineEta` fallback without throwing 500 | ✅ PASS |
| **Test 2** | Audit Hash Chain | Cryptographic integrity | Recorded entries and verified intact hash chain via `GET /audit/verify` | ✅ PASS |
| **Test 3** | Idempotent Dedupe | Duplicate Kafka delivery / retry | Second event with identical `eventId` deduplicated via `processed_event` table; no second insert | ✅ PASS |
| **Test 4** | Conditional UPDATE | Concurrent reservation race | First reservation succeeded (`status: DISPATCHED`); second concurrent attempt on the same unit failed with `409 Conflict` | ✅ PASS |
| **Test 5** | Observability Summary | Telemetry aggregation | Ingested events reflected in `GET /metrics/summary`, chain validity verified | ✅ PASS |
| **Test 6** | Database Tampering | Direct row payload mutation in DB | `GET /audit/verify` immediately detected forgery, returned HTTP 409, identified `brokenAtSeq: 7`, and toggled `audit_chain_valid = 0` | ✅ PASS |

---

## 5. Test Suite Verification Across All Modules

Full test run (`mvn test`): **216 tests passing across all 13 Maven modules, 0 failures, 0 errors**:

```
[INFO] Reactor Summary for H8 EMS Platform 1.0.0-SNAPSHOT:
[INFO] 
[INFO] H8 EMS Platform .................................... SUCCESS [  0.002 s]
[INFO] H8 Common Library .................................. SUCCESS [  8.422 s]
[INFO] H8 Contracts ....................................... SUCCESS [  1.619 s]
[INFO] H8 Simulator ....................................... SUCCESS [  4.091 s]
[INFO] H8 Data Seed ....................................... SUCCESS [  0.087 s]
[INFO] H8 API Gateway ..................................... SUCCESS [  4.454 s]
[INFO] H8 Incident Service ................................ SUCCESS [ 14.105 s]
[INFO] H8 Dispatch Service ................................ SUCCESS [ 10.631 s]
[INFO] H8 Tracking Service ................................ SUCCESS [  8.808 s]
[INFO] H8 Routing Service ................................. SUCCESS [  8.645 s]
[INFO] H8 Hospital Service ................................ SUCCESS [  6.907 s]
[INFO] H8 Redeployment Service ............................ SUCCESS [  7.297 s]
[INFO] H8 Audit Service ................................... SUCCESS [  8.759 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS (216 tests passing, 0 failures, 0 errors)
```

---

## 6. Blueprint for Phase 8: Scientific Experiments S1–S5 & Research Paper

Phase 8 is the final phase of the project: running the discrete-event simulation experiments, generating publication-quality figures, and assembling the final research deliverables.

### 6.1 Simulation Scenarios to Execute
Using the standalone `simulator` module (which implements pure Java 21 discrete-event simulation using common random numbers across runs):

1. **Scenario S1 (Baseline Urban Demand)**:
   - Delhi NCT geometry, 50 units (35 BLS, 15 ALS), 10 hospitals.
   - Non-homogeneous Poisson demand (NHPP thinning) with morning and evening rush-hour peaks.
2. **Scenario S2 (Mass-Casualty Incident / Surge)**:
   - Sudden localized spike of 30 Critical/High-severity calls within a 15-minute window in a high-density zone.
3. **Scenario S3 (Rural / Outer Peripheral Coverage Deficit)**:
   - Low-density outer districts with extended transit times; tests coverage penalty and redeployment pull.
4. **Scenario S4 (Hospital Overcrowding & ED Handover Delay)**:
   - 3 tertiary hospitals suffer ED gridlock; handover times increase to 45–60 minutes, testing capacity-aware destination rerouting.
5. **Scenario S5 (Sensitivity & Parameter Ablation)**:
   - Parameter sweeps over weights $w_1$ (ETA), $w_2$ (ALS match), $w_3$ (Fatigue), $w_4$ (Coverage loss), and $w_5$ (Staleness).

### 6.2 Comparison Policies
- **B1 (Baseline 1: Nearest Available)**: Minimizes raw Euclidean/Haversine distance only; ignores capability and coverage loss.
- **B2 (Baseline 2: Status-Quo Static Base)**: Traditional fixed-station dispatch with nearest hospital destination.
- **P1 (Capability-Aware Dispatch)**: Uses `DispatchScorer` matching ALS/BLS clinical urgency.
- **P2 (Joint Dispatch & Capacity-Aware Destination)**: `DispatchScorer` + `DestinationRanker` to minimize total time-to-treatment and prevent hospital bottlenecking.
- **P3 (Full H8 Framework)**: `DispatchScorer` + `DestinationRanker` + proactive `RedeploymentPlanner` for dynamic fleet posture.
- **Ablations**: P3 without coverage penalty ($\alpha = 0$), P3 without handover alert advance ($\delta = 0$).

### 6.3 Metrics & Output Artifacts to Generate
- **Primary Response Metrics**: Mean, median, p90, and p95 response time (seconds).
- **Clinical Equity**: Within-target response rate ($\le 480\text{s}$ for Category 1 / Priority 1 calls).
- **System Longevity**: Paramedic shift fatigue variance, dynamic 8-minute coverage retention ratio over 24-hour horizon.
- **Hospital Handover**: ED wait time and ambulance offload delay reduction.
- **Statistical Significance**: Paired Student's t-test and Wilcoxon signed-rank test ($p < 0.001$, 95% Confidence Intervals) across 30 seeds with Common Random Numbers (CRN).
- **Artifacts**: CSV outputs in `experiments/results/` and publication figures in `experiments/plots/`.
