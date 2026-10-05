# Chapter 2: Software Requirements Specification (SRS)

## 2.1 Scope & Purpose
This Software Requirements Specification (SRS) establishes the formal functional and non-functional requirements for the H8 Emergency Medical Dispatch Platform. It provides an unambiguous baseline for software engineers, clinical directors, auditors, and system architects.

---

## 2.2 Functional Requirements Matrix

### 2.2.1 Incident Management Subsystem (`incident-service`)
* **FR-INC-01: Call Intake & Anonymization**  
  The system shall ingest incoming emergency calls containing incident latitude, longitude, reported caller telephone number, incident type, and triage questions. The caller phone number shall be converted immediately into a salted SHA-256 hash using a server-side salt before database insertion. The plaintext phone number shall never be persisted or logged.
* **FR-INC-02: Deterministic Triage Categorization**  
  The system shall categorize incident clinical severity into one of four standardized priority classifications based on MPDS triage protocols:
  * `CRITICAL` (Immediate life threat; cardiac arrest, major arterial hemorrhage, respiratory cessation).
  * `URGENT` (Potentially unstable; stroke symptoms within 4-hour window, open fractures, severe sepsis).
  * `STANDARD` (Stable acute illness; minor trauma, isolated limb injury, controlled bleeding).
  * `NON_EMERGENCY` (Scheduled medical transport, minor dressing changes).
* **FR-INC-03: Clinical Need Classification**  
  The system shall evaluate primary clinical needs: `CARDIAC`, `TRAUMA`, `STROKE`, `PEDIATRIC`, `RESPIRATORY`, or `GENERAL`.
* **FR-INC-04: Incident Lifecycle State Management**  
  Incidents shall transition through a strict state machine: `REPORTED` -> `TRIAGED` -> `DISPATCHED` -> `ON_SCENE` -> `TRANSPORTING` -> `RESOLVED` (or `CANCELLED`). Illegal transitions shall return HTTP 409 Conflict.
* **FR-INC-05: Incident Outbox Emission**  
  Upon creation or status change, an outbox event `IncidentCreatedEvent` or `IncidentStatusChangedEvent` shall be persisted within the same database transaction.

### 2.2.2 Dispatch Optimization Subsystem (`dispatch-service`)
* **FR-DIS-01: Candidate Unit Retrieval & Filtering**  
  Upon receiving dispatch evaluation requests, the system shall query all ambulances whose status is currently `AVAILABLE` within a configurable geographical search radius (default: 25 km, expandable to 50 km for rural fringes).
* **FR-DIS-02: Multi-Factor Candidate Scoring**  
  The system shall compute an overall score $S(u, i) \in [0, 1]$ for every candidate ambulance $u$ against incident $i$ utilizing the pure Java `DispatchScorer` engine.
* **FR-DIS-03: Capability Requirement Enforcement**  
  If an incident is flagged as `requiresAls = true`, any BLS unit evaluated shall receive a mandatory clinical penalty ($\Delta_{cap} = 0.55$) in the scoring formula.
* **FR-DIS-04: Atomic Unit Reservation**  
  When an operator or automatic dispatcher issues a dispatch command for unit $u$, the system shall execute an atomic SQL update:
  ```sql
  UPDATE ambulance_units 
  SET status = 'ASSIGNED', version = version + 1 
  WHERE id = :unitId AND status = 'AVAILABLE';
  ```
  If zero rows are updated, the dispatch command shall abort with an `AmbulanceUnavailableException` and trigger re-ranking.
* **FR-DIS-05: Manual Dispatch Override & Audit Trail**  
  Dispatch supervisors may override algorithmic recommendations to select an alternate unit. In such cases, the system shall require an explicit textual justification reason and emit an `OVERRIDE` audit record containing both the algorithmic choice and the operator's manual selection.

### 2.2.3 Real-Time Vehicle Tracking & Telematics (`tracking-service`)
* **FR-TRK-01: High-Frequency GPS Ingestion**  
  The system shall accept continuous GPS telemetry updates (`unitId`, `latitude`, `longitude`, `speedKmH`, `bearingDegrees`, `timestamp`) via HTTP REST and WebSocket connections at frequencies up to 1 Hz per active vehicle.
* **FR-TRK-02: Spatial Indexing with Redis Geo**  
  Every validated location update shall be persisted into Redis using geospatial commands (`GEOADD units:geo lon lat unitId`) with a strict Time-To-Live (TTL) of 60 seconds.
* **FR-TRK-03: Dead-Reckoning Extrapolation**  
  When a unit’s GPS fix is older than 5 seconds but newer than 60 seconds, the system shall extrapolate its current estimated position along its active route geometry using its last known speed and bearing.
* **FR-TRK-04: Telemetry Staleness Flagging**  
  Any location telemetry older than 60 seconds shall be flagged as `STALE`. If stale, the candidate scorer shall apply a staleness degradation penalty ($P_{stale} = 1.0$), deprioritizing the unit until fresh telemetry is received.

### 2.2.4 Road Routing & Travel Time Matrix (`routing-service`)
* **FR-ROU-01: GraphHopper Road Network Routing**  
  The routing engine shall compute turn-by-turn routes and driving times using an embedded GraphHopper road graph of the metropolitan area rather than straight-line approximations.
* **FR-ROU-02: Emergency Vehicle Weighting Profile**  
  Calculations shall utilize a specialized emergency vehicle profile that factors in priority passage, elevated average speeds on arterial highways, and realistic turn penalties in historical city corridors.
* **FR-ROU-03: Fallback Distance Calculation**  
  If the GraphHopper engine is unreachable or timeout occurs (>300 ms), the system shall automatically fall back to Haversine great-circle calculation with an urban tortuosity factor ($\tau = 1.35$) and standard 40 km/h emergency speed.

### 2.2.5 Hospital Emergency Department & Diversion Management (`hospital-service`)
* **FR-HOS-01: Capacity Telemetry Tracking**  
  The system shall maintain real-time telemetry for all registered receiving hospitals: total ED beds, free ED beds, staffed ICU beds, available ventilators, and active surgical trauma bays.
* **FR-HOS-02: Diversion Protocol Management**  
  Hospitals shall have the capability to trigger a diversion status (`DIVERSION = true`) when emergency department bed capacity reaches 100% or when critical diagnostic equipment (e.g. CT scanner, Cath lab) suffers an outage.
* **FR-HOS-03: Destination Hospital Ranking**  
  Upon paramedic request, the `DestinationRanker` shall score and rank all receiving hospitals based on clinical capability alignment (e.g., Cath lab for cardiac arrest, neuro-ICU for stroke), travel time, diversion status, and offload delays.
* **FR-HOS-04: Real-Time Pre-Arrival Notifications (AlertHub SSE)**  
  When an ambulance begins transporting a patient toward a hospital, the system shall stream Server-Sent Events (SSE) to the receiving hospital's triage console with patient acuity, estimated arrival time (ETA), and vital signs.

### 2.2.6 Dynamic Fleet Redeployment (`redeployment-service`)
* **FR-RED-01: Real-Time Coverage Analysis**  
  The system shall continuously evaluate metropolitan coverage using the Maximum Expected Coverage Location Problem (MEXCLP) model, calculating the fraction of demand zones covered within an 8-minute response threshold.
* **FR-RED-02: Relocation Recommendations**  
  When an ambulance is dispatched to an incident, leaving a suburban quadrant uncovered, the engine shall compute optimal move-up relocations for idle units to re-balance coverage and minimize secondary response delays.

### 2.2.7 Cryptographic Dispatch Audit (`audit-service`)
* **FR-AUD-01: Immutable Event Ledger**  
  Every dispatch event, clinical triage, manual override, unit assignment, and status transition shall be recorded in an append-only cryptographic ledger.
* **FR-AUD-02: Cryptographic Hash Chaining**  
  Each block in the ledger shall contain the SHA-256 hash of the preceding block:
  $$H_i = \text{SHA256}(H_{i-1} \parallel \text{EventUUID}_i \parallel \text{PayloadJSON}_i \parallel \text{TimestampEpoch}_i)$$
* **FR-AUD-03: Mathematical Integrity Verification**  
  The system shall provide an endpoint `/audit/verify` that recalculates the entire hash chain from Genesis ($H_0$) to the latest block, mathematically validating that zero records have been inserted, deleted, or altered.

---

## 2.3 Non-Functional Requirements (NFR)

### 2.3.1 Performance & Latency Thresholds
* **NFR-PERF-01: Candidate Ranking Latency**  
  The `dispatch-service` shall compute capability-aware ranking scores for a fleet of 50 active ambulances in under **50 milliseconds** (P95) and under **100 milliseconds** (P99).
* **NFR-PERF-02: Telematics Ingestion Throughput**  
  The `tracking-service` shall support sustained ingestion of at least **1,000 GPS telemetry events per second** with response latency under 15 milliseconds.
* **NFR-PERF-03: Gateway Routing Overhead**  
  The API Gateway shall introduce less than **5 milliseconds** of latency overhead for proxied requests under normal operating loads.

### 2.3.2 Reliability, Availability & Fault Tolerance
* **NFR-REL-01: High Availability Target**  
  The system architecture shall target 99.99% operational uptime (maximum allowable unscheduled downtime: 52.6 minutes/year).
* **NFR-REL-02: Circuit Breaker & Graceful Degradation**  
  All inter-service HTTP interactions shall be protected by Resilience4j circuit breakers. If `routing-service` fails, `dispatch-service` shall fall back instantly to spatial Haversine calculations without dropping active dispatch requests.
* **NFR-REL-03: At-Least-Once Delivery with Idempotency**  
  Kafka event streaming shall guarantee at-least-once message delivery. Every consumer shall verify message idempotency by querying a `processed_events` table before applying state updates.

### 2.3.3 Security, Privacy & Compliance
* **NFR-SEC-01: Role-Based Access Control (RBAC)**  
  All microservice APIs shall authenticate incoming requests against Keycloak OAuth2 / OpenID Connect tokens. Permissions shall be strictly enforced across four roles:
  * `DISPATCHER`: Can create incidents, trigger dispatch, override units.
  * `CREW`: Can update unit operational status, submit vitals, request destinations.
  * `HOSPITAL_STAFF`: Can update bed counts, toggle diversion, view incoming pre-arrivals.
  * `AUDITOR`: Read-only access to cryptographic ledger and compliance reports.
* **NFR-SEC-02: Patient Privacy (HIPAA / DISHA)**  
  No patient identifiable information (PHI/PII) such as full legal name, national identity number, or unhashed telephone number shall be transmitted over unencrypted channels or stored in unencrypted databases.
* **NFR-SEC-03: Transport Layer Security**  
  All external communications (client-to-gateway, gateway-to-services, cloud tunnels) shall enforce TLS 1.3 encryption.
