# Chapter 5: Complete UML Modeling Suite

## 5.1 Overview of UML Architectural Models
This chapter provides the formal Unified Modeling Language (UML 2.5) specifications for the H8 Emergency Medical Dispatch Platform. It presents complete behavioral, structural, relational, and physical deployment views:

* **Use Case Modeling**: Actor interactions and operational boundaries.
* **Structural Modeling**: Package architecture, component mesh, and domain class structures.
* **Behavioral Modeling**: End-to-end sequence workflows, activity lifecycles, and finite state machines.
* **Data & Physical Modeling**: Entity-Relationship (ER) schemas and production deployment topology.

---

## 5.2 UML Model 1: System Use Case Diagram

```mermaid
graph TD
    subgraph System Boundary: H8 EMS Emergency Dispatch Platform
        UC1(Ingest Emergency Call)
        UC2(Perform MPDS Clinical Triage)
        UC3(Anonymize Caller PII / Salted Hash)
        UC4(Query Ranked Ambulance Candidates)
        UC5(Execute Atomic Unit Reservation)
        UC6(Manual Dispatcher Override)
        UC7(Transmit Live GPS Telemetry)
        UC8(Update Paramedic Mission State)
        UC9(Submit Patient Clinical Vitals)
        UC10(Request Ranked Destination Hospital)
        UC11(Receive Pre-Arrival Alert / SSE)
        UC12(Update Bed Capacity & Diversion)
        UC13(Compute Fleet MEXCLP Coverage)
        UC14(Verify SHA-256 Cryptographic Audit Ledger)
    end

    Caller[Emergency Caller / Bystander] --> UC1
    
    Dispatcher[911 / 108 Emergency Dispatcher] --> UC2
    Dispatcher --> UC4
    Dispatcher --> UC5
    Dispatcher --> UC6
    Dispatcher --> UC13

    UC1 -.-> UC3
    UC2 -.-> UC4
    UC5 -.-> UC14
    UC6 -.-> UC14

    Crew[Paramedic Ambulance Crew] --> UC7
    Crew --> UC8
    Crew --> UC9
    Crew --> UC10

    Doctor[Hospital ED Triage Staff] --> UC11
    Doctor --> UC12

    Auditor[Legal & Compliance Auditor] --> UC14
```

---

## 5.3 UML Model 2: Package & Maven Multi-Module Hierarchy

```mermaid
graph TD
    subgraph Maven Multi-Module Architecture: root pom.xml
        root[h8-ems-root]
        
        subgraph Pure Java 21 Modules - Zero External Frameworks
            common[common: Algorithmic Core]
            contracts[contracts: DTOs & Events]
            simulator[simulator: Monte Carlo Engine]
        end
        
        subgraph Spring Boot 3 Microservices
            gateway[api-gateway :8080]
            incident[incident-service :8081]
            dispatch[dispatch-service :8082]
            tracking[tracking-service :8083]
            routing[routing-service :8084]
            hospital[hospital-service :8085]
            redeploy[redeployment-service :8086]
            audit[audit-service :8087]
        end
    end

    root --> common
    root --> contracts
    root --> simulator
    root --> gateway
    root --> incident
    root --> dispatch
    root --> tracking
    root --> routing
    root --> hospital
    root --> redeploy
    root --> audit

    contracts --> common
    simulator --> common
    simulator --> contracts
    
    incident --> contracts
    dispatch --> common
    dispatch --> contracts
    tracking --> contracts
    hospital --> common
    hospital --> contracts
    redeploy --> common
    redeploy --> contracts
    audit --> contracts
```

---

## 5.4 UML Model 3: System Component Diagram

```mermaid
graph TB
    subgraph Presentation Tier
        UI_Home[Portal Hub :8088]
        UI_Disp[Dispatcher Console]
        UI_Crew[Crew PWA Portal]
        UI_ED[ED Bed Board]
    end

    subgraph Edge & Security
        Nginx[Nginx / Reverse Proxy :8088]
        Gateway[Spring Cloud Gateway :8080]
        Keycloak[Keycloak 25 IAM :8180]
    end

    subgraph Microservices Tier
        MS_Inc[incident-service :8081]
        MS_Dis[dispatch-service :8082]
        MS_Trk[tracking-service :8083]
        MS_Rou[routing-service :8084]
        MS_Hos[hospital-service :8085]
        MS_Red[redeployment-service :8086]
        MS_Aud[audit-service :8087]
    end

    subgraph Data & Messaging Backbone
        PG[(PostgreSQL 15 PostGIS)]
        Kafka{{Apache Kafka 3.7 Bus}}
        Redis[(Redis 7 Geo)]
    end

    UI_Home --> Nginx
    UI_Disp --> Nginx
    UI_Crew --> Nginx
    UI_ED --> Nginx

    Nginx --> Gateway
    Gateway --> Keycloak
    
    Gateway --> MS_Inc
    Gateway --> MS_Dis
    Gateway --> MS_Trk
    Gateway --> MS_Rou
    Gateway --> MS_Hos
    Gateway --> MS_Red
    Gateway --> MS_Aud

    MS_Dis -.->|Sync Route Query| MS_Rou

    MS_Inc --> PG
    MS_Dis --> PG
    MS_Hos --> PG
    MS_Red --> PG
    MS_Aud --> PG

    MS_Dis --> Redis
    MS_Trk --> Redis

    MS_Inc -->|Outbox| Kafka
    MS_Dis -->|Outbox| Kafka
    MS_Hos -->|Outbox| Kafka
    MS_Trk -->|Telemetry| Kafka

    Kafka -->|Consume| MS_Dis
    Kafka -->|Consume| MS_Trk
    Kafka -->|Consume| MS_Aud
    Kafka -->|Consume| MS_Red
```

---

## 5.5 UML Model 4: Class Diagram — Core Domain Model (`common`)

```mermaid
classDiagram
    class DispatchScorer {
        +ALPHA : double = 0.35
        +BETA : double = 0.45
        +GAMMA : double = 0.08
        +DELTA : double = 0.07
        +EPSILON : double = 0.05
        +score(AmbulanceUnit, Incident, RouteInfo, double) CandidateScore
        -calcCapabilityPenalty(CapabilityType, Severity, boolean) double
        -calcFatigueFactor(int, long) double
        -calcStaleness(long, long) double
    }

    class CoverageModel {
        +evaluateCoverage(List~AmbulanceUnit~, List~DemandNode~, double) CoverageResult
        +computeMexclp(List~Station~, int, double) RelocationPlan
    }

    class DestinationRanker {
        +rankHospitals(PatientProfile, List~Hospital~, Location) List~RankedHospital~
        -scoreHospital(Hospital, PatientProfile, double) double
    }

    class AmbulanceUnit {
        -UUID id
        -String callSign
        -UnitType type
        -UnitStatus status
        -Location currentLocation
        -int shiftMissionCount
        -long activeShiftMinutes
        -Instant lastTelemetryTime
        +isAvailable() boolean
        +canProvideAls() boolean
    }

    class Incident {
        -UUID id
        -Location location
        -Severity severity
        -ClinicalNeed need
        -boolean requiresAls
        -IncidentStatus status
        -Instant createdAt
    }

    class Hospital {
        -UUID id
        -String name
        -Location location
        -Set~CapabilityType~ capabilities
        -int totalEdBeds
        -int freeEdBeds
        -int freeIcuBeds
        -boolean diversionActive
    }

    class Location {
        -double latitude
        -double longitude
        -Instant timestamp
        +distanceTo(Location) double
    }

    class UnitType {
        <<enumeration>>
        ALS
        BLS
    }

    class UnitStatus {
        <<enumeration>>
        AVAILABLE
        ASSIGNED
        EN_ROUTE
        ON_SCENE
        TRANSPORTING
        AT_HOSPITAL
        MAINTENANCE
    }

    class Severity {
        <<enumeration>>
        CRITICAL
        URGENT
        STANDARD
        NON_EMERGENCY
    }

    AmbulanceUnit --> UnitType
    AmbulanceUnit --> UnitStatus
    AmbulanceUnit --> Location
    Incident --> Severity
    Incident --> Location
    Hospital --> Location
    DispatchScorer ..> AmbulanceUnit : scores
    DispatchScorer ..> Incident : against
    DestinationRanker ..> Hospital : ranks
```

---

## 5.6 UML Model 5: Class Diagram — Dispatch Service & Persistence (`dispatch-service`)

```mermaid
classDiagram
    class DispatchController {
        -DispatchService dispatchService
        +getCandidates(double, double, Severity, Need, boolean) ResponseEntity
        +assignUnit(DispatchRequest) ResponseEntity
        +overrideUnit(OverrideRequest) ResponseEntity
    }

    class DispatchService {
        -AmbulanceUnitRepository unitRepo
        -AssignmentRepository assignmentRepo
        -OutboxRepository outboxRepo
        -RoutingClient routingClient
        -DispatchScorer scorer
        +findRankedCandidates(Location, Severity, Need, boolean) List~CandidateDTO~
        +dispatchUnit(UUID, UUID) AssignmentResult
        +manualOverride(UUID, UUID, String) AssignmentResult
    }

    class AmbulanceUnitEntity {
        -UUID id
        -String callSign
        -String type
        -String status
        -Point location
        -int shiftMissions
        -long version
        +reserveUnit() boolean
    }

    class AssignmentEntity {
        -UUID id
        -UUID incidentId
        -UUID unitId
        -String status
        -Instant assignedAt
        -Instant clearedAt
    }

    class OutboxEventEntity {
        -UUID id
        -String aggregateType
        -UUID aggregateId
        -String eventType
        -String payloadJson
        -String status
        -Instant createdAt
    }

    class ProcessedEventEntity {
        -UUID eventId
        -String sourceTopic
        -Instant processedAt
    }

    DispatchController --> DispatchService
    DispatchService --> AmbulanceUnitEntity
    DispatchService --> AssignmentEntity
    DispatchService --> OutboxEventEntity
    DispatchService --> ProcessedEventEntity
```

---

## 5.7 UML Model 6: Sequence Diagram — Incident Intake & Automated Triage

```mermaid
sequenceDiagram
    autonumber
    actor Caller as Emergency Caller
    participant GW as API Gateway (:8080)
    participant IS as incident-service (:8081)
    participant Hash as SaltedHasher
    participant DB as PostgreSQL (incident)
    participant Kafka as Apache Kafka

    Caller->>GW: POST /incidents (callerPhone, lat, lon, triageAnswers)
    activate GW
    GW->>IS: Forward Request with Validated Auth
    activate IS

    IS->>Hash: hashPhone(callerPhone, SERVER_SALT)
    activate Hash
    Hash-->>IS: Return Salted SHA-256 Digest (e.g. 5e884898da28...)
    deactivate Hash

    IS->>IS: Evaluate MPDS Protocol (Determine: CRITICAL, CARDIAC, requiresAls=true)
    
    IS->>DB: BEGIN TRANSACTION
    IS->>DB: INSERT INTO incidents (id, phone_hash, lat, lon, severity, need, status='CREATED')
    IS->>DB: INSERT INTO outbox (event_type='IncidentCreatedEvent', payload={...})
    IS->>DB: COMMIT TRANSACTION

    IS-->>GW: HTTP 201 Created (incidentId: "0000-...")
    deactivate IS
    GW-->>Caller: Confirmation & Estimated Response Initialized
    deactivate GW

    Note over IS,Kafka: Asynchronous Outbox Relay publishes event to Kafka
    IS->>Kafka: Publish to topic 'incident.created'
```

---

## 5.8 UML Model 7: Sequence Diagram — Candidate Scoring & Atomic Reservation

```mermaid
sequenceDiagram
    autonumber
    actor Dispatcher as Emergency Dispatcher
    participant GW as API Gateway
    participant DS as dispatch-service
    participant RS as routing-service
    participant Scorer as DispatchScorer (common)
    participant DB as PostgreSQL (dispatch)
    participant Kafka as Apache Kafka

    Dispatcher->>GW: GET /dispatch/candidates?lat=26.91&lon=75.81&severity=CRITICAL&requiresAls=true
    GW->>DS: Forward Request
    activate DS

    DS->>DB: SELECT * FROM ambulance_units WHERE status = 'AVAILABLE'
    DB-->>DS: Return 14 candidate units

    DS->>RS: POST /routes/matrix (Candidate Locations -> Incident)
    activate RS
    RS-->>DS: Return Driving ETAs & Distances (GraphHopper)
    deactivate RS

    loop For each candidate unit
        DS->>Scorer: score(unit, incident, routeInfo, systemBusyFrac)
        Scorer-->>DS: Return candidate score + clinical breakdown
    end

    DS->>DS: Sort candidates descending by score (ALS units prioritized)
    DS-->>Dispatcher: HTTP 200 OK (Ranked Candidate List)
    deactivate DS

    Dispatcher->>GW: POST /dispatch (incidentId, unitId='AMB-03')
    GW->>DS: Forward Assignment Command
    activate DS

    DS->>DB: BEGIN TRANSACTION
    Note over DS,DB: Hard Rule #3: Conditional UPDATE ensures atomic lock
    DS->>DB: UPDATE ambulance_units SET status='ASSIGNED' WHERE id='AMB-03' AND status='AVAILABLE'
    
    alt Zero rows updated (Conflict: unit claimed by parallel call)
        DB-->>DS: 0 rows updated
        DS->>DB: ROLLBACK
        DS-->>Dispatcher: HTTP 409 Conflict (Ambulance Already Committed)
    else 1 row updated (Lock Acquired)
        DB-->>DS: 1 row updated
        DS->>DB: INSERT INTO assignments (id, incident_id, unit_id, status='ASSIGNED')
        DS->>DB: INSERT INTO outbox (event_type='UnitDispatchedEvent', payload={...})
        DS->>DB: COMMIT TRANSACTION
        DS-->>Dispatcher: HTTP 200 OK (Unit Assigned Successfully)
        DS->>Kafka: Publish 'unit.dispatched' via Outbox Relay
    end
    deactivate DS
```

---

## 5.9 UML Model 8: Sequence Diagram — High-Frequency GPS Ingestion

```mermaid
sequenceDiagram
    autonumber
    actor Amb as Ambulance IoT / Mobile PWA
    participant GW as API Gateway
    participant TS as tracking-service
    participant Redis as Redis 7 In-Memory
    participant Kafka as Apache Kafka

    loop Every 1 to 5 seconds
        Amb->>GW: POST /tracking/location (unitId, lat, lon, speed, bearing, timestamp)
        GW->>TS: Forward Telemetry
        activate TS

        TS->>TS: Validate Lat/Lon bounds & Timestamp freshness
        
        TS->>Redis: GEOADD units:geo lon lat unitId
        TS->>Redis: HSET units:telemetry:unitId speed ... bearing ... lastSeen (TTL: 60s)
        
        TS->>Kafka: Publish to 'unit.location.updated'
        TS-->>Amb: HTTP 200 OK (Telemetry Ingested)
        deactivate TS
    end
```

---

## 5.10 UML Model 9: Sequence Diagram — Hospital Destination Ranking & Pre-Arrival Handover

```mermaid
sequenceDiagram
    autonumber
    actor Crew as Paramedic Crew (PWA)
    participant GW as API Gateway
    participant HS as hospital-service
    participant Ranker as DestinationRanker (common)
    participant DB as PostgreSQL (hospital)
    participant AlertHub as AlertHub (SSE Emitter)
    actor EDStaff as Hospital ED Staff (Console)

    Crew->>GW: GET /hospitals/rank?need=TRAUMA&severity=CRITICAL&lat=...&lon=...
    GW->>HS: Forward Query
    activate HS

    HS->>DB: SELECT * FROM hospitals (Include bed counts, capabilities, diversion)
    DB-->>HS: 5 Tier-1 Jaipur Hospitals

    HS->>Ranker: rankHospitals(patientProfile, hospitals, crewLocation)
    Ranker-->>HS: Return Ranked Hospitals (SMS Apex Trauma Ranked #1)
    HS-->>Crew: HTTP 200 OK (Ranked List with Trauma Beds & Cath Lab status)
    deactivate HS

    Crew->>GW: POST /hospitals/handover (hospitalId='SMS', eta=380s, vitals={BP: '120/80', SpO2: 98, HR: 84})
    GW->>HS: Submit Handover Alert
    activate HS

    HS->>DB: INSERT INTO pre_arrival_alerts (id, hospital_id, unit_id, vitals, eta)
    HS->>AlertHub: broadcastAlert('SMS', alertData)
    activate AlertHub
    AlertHub->>EDStaff: Server-Sent Event (SSE: 'pre-arrival')
    deactivate AlertHub
    HS-->>Crew: HTTP 200 OK (Alert Acknowledged by ED Triage)
    deactivate HS
```

---

## 5.11 UML Model 10: State Machine Diagram — Ambulance Unit Operational Lifecycle

```mermaid
stateDiagram-v2
    [*] --> AVAILABLE : Unit Enters Service / Login

    AVAILABLE --> ASSIGNED : Conditional Atomic UPDATE (Dispatch Event)
    
    ASSIGNED --> EN_ROUTE : Paramedic Acknowledges Assignment (Mobile PWA)
    
    EN_ROUTE --> ON_SCENE : Unit Arrives at Incident Geofence
    
    ON_SCENE --> TRANSPORTING : Patient Stabilized & Loaded (Destination Selected)
    
    TRANSPORTING --> AT_HOSPITAL : Ambulance Arrives at Receiving ED Bay
    
    AT_HOSPITAL --> CLEARING : Clinical Handover Complete (Decontamination / Paperwork)
    
    CLEARING --> AVAILABLE : Crew Marks Unit Back in Service
    
    ASSIGNED --> AVAILABLE : Incident Cancelled by Caller/Dispatcher
    
    AVAILABLE --> MAINTENANCE : Mechanical Fault / Restocking Required
    MAINTENANCE --> AVAILABLE : Maintenance Cleared
```

---

## 5.12 UML Model 11: State Machine Diagram — Incident Emergency Lifecycle

```mermaid
stateDiagram-v2
    [*] --> REPORTED : Incoming Emergency Call Received
    
    REPORTED --> TRIAGED : MPDS Severity & Clinical Need Determined
    
    TRIAGED --> DISPATCHED : Ambulance Assigned via Atomic Reservation
    
    DISPATCHED --> ON_SCENE : First Responder Arrives at Patient
    
    ON_SCENE --> TRANSPORTING : Patient En Route to Receiving Facility
    
    TRANSPORTING --> RESOLVED : Patient Transferred to Hospital Emergency Team
    
    ON_SCENE --> RESOLVED : Treated on Scene / Refused Transport
    
    REPORTED --> CANCELLED : False Alarm / Duplicate Call
    TRIAGED --> CANCELLED : Cancelled Prior to Dispatch
```

---

## 5.13 UML Model 12: Activity Diagram — End-to-End Emergency Dispatch Workflow

```mermaid
graph TD
    Start([Emergency Call Initiated]) --> Intake[Intake Incident Details & Location]
    Intake --> Hash[Salt & Hash Caller Phone Number]
    Hash --> Triage[Perform Clinical Triage: Determine Severity & ALS Need]
    Triage --> CandidateQuery[Query Available Fleet within Search Radius]
    CandidateQuery --> RoutingQuery[Fetch Real-Time Road Driving Times via GraphHopper]
    RoutingQuery --> Scoring[Compute DispatchScorer Multi-Factor Mathematical Score]
    Scoring --> Rank[Rank Candidates by Score]
    
    Rank --> Decision{Automatic or Manual Dispatch?}
    Decision -->|Automatic| SelectTop[Select Top Ranked Candidate]
    Decision -->|Manual Override| SupervisorChoice[Dispatcher Overrides Selection]
    SupervisorChoice --> AuditOverride[Record Explicit Justification Reason in Audit Log]
    AuditOverride --> ExecuteLock
    SelectTop --> ExecuteLock

    ExecuteLock[Execute Atomic Conditional UPDATE on Unit]
    ExecuteLock --> LockCheck{Lock Acquired?}
    LockCheck -->|No - 0 Rows Updated| ReRank[Ambulance Assigned Elsewhere - Re-query Fleet]
    ReRank --> CandidateQuery
    LockCheck -->|Yes - 1 Row Updated| RecordOutbox[Record Outbox Event in Database]
    
    RecordOutbox --> KafkaPub[Publish 'unit.dispatched' Event to Kafka]
    KafkaPub --> NotifyCrew[Push Mission Alert to Paramedic PWA]
    NotifyCrew --> CrewAck[Crew Accepts Mission & En Route]
    CrewAck --> OnScene[Crew Arrives On Scene & Stabilizes Patient]
    OnScene --> DestRank[Rank Receiving Hospitals by Trauma/Cardiac Capability]
    DestRank --> Handover[Initiate Pre-Arrival Notification via SSE to ED]
    Handover --> Transport[Transport Patient to Emergency Department]
    Transport --> EDTransfer[Transfer Patient to Trauma Team]
    EDTransfer --> Clear[Decontaminate & Mark Ambulance AVAILABLE]
    Clear --> End([Cycle Complete - Fleet Ready])
```

---

## 5.14 UML Model 13: Deployment Diagram — Production Cloud Topology

```mermaid
graph TB
    subgraph Internet & Public Access
        PublicUser[Public Mobile & Web Clients]
    end

    subgraph Cloud Infrastructure / Virtual Private Server (VPS)
        subgraph Ingress & SSL
            CF[Cloudflare Edge / Caddy TLS 1.3]
            NginxIngress[Nginx Reverse Proxy :8088]
        end

        subgraph Docker Bridge Network: h8-net
            GW_Cont[api-gateway :8080]
            
            subgraph Core Services
                IS_Cont[incident-service :8081]
                DS_Cont[dispatch-service :8082]
                TS_Cont[tracking-service :8083]
                RS_Cont[routing-service :8084]
                HS_Cont[hospital-service :8085]
                RD_Cont[redeployment-service :8086]
                AS_Cont[audit-service :8087]
            end

            subgraph Data & Messaging Containers
                PG_Cont[(PostgreSQL 15 PostGIS :5432)]
                Redis_Cont[(Redis 7 Geo :6379)]
                Kafka_Cont{{Apache Kafka 3.7 KRaft :9092}}
                KC_Cont[Keycloak 25 IAM :8180]
            end

            subgraph Monitoring
                Prom_Cont[Prometheus :9090]
                Graf_Cont[Grafana :3000]
            end
        end

        subgraph Persistent Docker Volumes
            Vol_PG[(pgdata Volume)]
            Vol_Graph[(graphhopper-data Volume)]
        end
    end

    PublicUser -->|HTTPS :443| CF
    CF -->|HTTP :8088| NginxIngress
    NginxIngress -->|Proxy /| GW_Cont

    GW_Cont --> CoreServices
    CoreServices --> DataContainers
    
    PG_Cont --> Vol_PG
    RS_Cont --> Vol_Graph

    Prom_Cont -.->|Scrape /actuator/prometheus| CoreServices
    Graf_Cont --> Prom_Cont
```

---

## 5.15 UML Model 14: Entity-Relationship (ER) Diagram — Relational Database Schema

```mermaid
erDiagram
    INCIDENTS {
        uuid id PK
        varchar phone_hash
        double_precision latitude
        double_precision longitude
        varchar severity
        varchar clinical_need
        boolean requires_als
        varchar status
        timestamp created_at
        timestamp updated_at
    }

    AMBULANCE_UNITS {
        uuid id PK
        varchar call_sign UK
        varchar vehicle_type
        varchar status
        geometry location
        int shift_mission_count
        bigint active_shift_minutes
        timestamp last_telemetry_at
        bigint version
    }

    ASSIGNMENTS {
        uuid id PK
        uuid incident_id FK
        uuid unit_id FK
        varchar status
        timestamp assigned_at
        timestamp on_scene_at
        timestamp resolved_at
    }

    HOSPITALS {
        uuid id PK
        varchar name
        geometry location
        varchar capabilities
        int total_ed_beds
        int free_ed_beds
        int free_icu_beds
        boolean diversion_active
        timestamp updated_at
    }

    PRE_ARRIVAL_ALERTS {
        uuid id PK
        uuid hospital_id FK
        uuid unit_id FK
        jsonb clinical_vitals
        int eta_seconds
        timestamp created_at
    }

    OUTBOX_EVENTS {
        uuid id PK
        varchar aggregate_type
        uuid aggregate_id
        varchar event_type
        jsonb payload
        varchar status
        timestamp created_at
    }

    PROCESSED_EVENTS {
        uuid event_id PK
        varchar topic
        timestamp processed_at
    }

    AUDIT_LEDGER {
        bigint sequence_id PK
        varchar block_hash UK
        varchar previous_hash
        uuid event_id
        varchar event_type
        jsonb event_payload
        timestamp created_at
    }

    INCIDENTS ||--o{ ASSIGNMENTS : receives
    AMBULANCE_UNITS ||--o{ ASSIGNMENTS : committed_to
    HOSPITALS ||--o{ PRE_ARRIVAL_ALERTS : receives_alerts
    AMBULANCE_UNITS ||--o{ PRE_ARRIVAL_ALERTS : transmits_alerts
```
