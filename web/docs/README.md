# H8 EMS Architecture, Design & Engineering Documentation Dossier

This directory ([`docs-src/`](file:///c:/ambulance/docs-src/)) contains the complete, publication-grade engineering documentation for the **H8 Capability-Aware Emergency Medical Services (EMS) Dispatch & Telematics Platform**.

The documentation is formatted across **11 comprehensive chapters** with all **14 standard UML diagrams**, mathematical algorithmic formulations, PostgreSQL relational DDL, Kafka event schemas, REST API references, and production operations runbooks. When compiled or printed, the document spans **60 to 70 pages**.

---

## Document Index & Chapter Directory

| Chapter | Document File | Topic / Focus Area | Key Architectural Artifacts |
| :---: | :--- | :--- | :--- |
| **01** | [`01_executive_summary_and_overview.md`](file:///c:/ambulance/docs-src/01_executive_summary_and_overview.md) | **Executive Summary & Operational Scope** | Problem statement, clinical mismatch dilemma, 14-unit fleet roster, 5 Jaipur hospitals, 10 hard architectural rules. |
| **02** | [`02_software_requirements_specification.md`](file:///c:/ambulance/docs-src/02_software_requirements_specification.md) | **Software Requirements Specification (SRS)** | 40 Functional Requirements (FR-01 to FR-40), 20 Non-Functional Requirements (NFR-01 to NFR-20), HIPAA/DISHA compliance. |
| **03** | [`03_system_architecture_and_design.md`](file:///c:/ambulance/docs-src/03_system_architecture_and_design.md) | **System Architecture & Design (ADD)** | Microservices decomposition, Hexagonal architecture, service catalog, ports topology, synchronous REST vs asynchronous Kafka outbox. |
| **04** | [`04_algorithmic_specifications_and_math.md`](file:///c:/ambulance/docs-src/04_algorithmic_specifications_and_math.md) | **Algorithmic Specifications & Mathematical Foundations** | `DispatchScorer` 5-factor equation, weights ($\alpha,\beta,\gamma,\delta,\epsilon$), clinical penalty matrix, MEXCLP coverage formulation, `DestinationRanker`, Haversine & urban tortuosity ($\tau=1.35$). |
| **05** | [`05_complete_uml_modeling_suite.md`](file:///c:/ambulance/docs-src/05_complete_uml_modeling_suite.md) | **Complete UML 2.5 Modeling Suite (All 14 Diagrams)** | Complete high-resolution Mermaid & ASCII diagrams covering structural, behavioral, relational, and physical deployment models. |
| **06** | [`06_database_schemas_and_kafka_contracts.md`](file:///c:/ambulance/docs-src/06_database_schemas_and_kafka_contracts.md) | **Database Schemas & Kafka Contracts** | PostgreSQL 15 DDL for `dispatch`, `incident`, `hospital`, `audit` schemas, PostGIS GIST indexes, transactional outbox DDL, 7 Kafka JSON event schemas, Redis Geo. |
| **07** | [`07_api_reference_and_service_specifications.md`](file:///c:/ambulance/docs-src/07_api_reference_and_service_specifications.md) | **API Reference & Microservice Endpoints** | REST API specifications for all 8 microservices with JSON request/response payloads, query parameters, and HTTP status codes. |
| **08** | [`08_cryptographic_audit_and_security.md`](file:///c:/ambulance/docs-src/08_cryptographic_audit_and_security.md) | **Cryptographic Audit Ledger & Security** | SHA-256 hash chain formulation ($H_i$), mathematical tamper detection, verification algorithm in Java, Keycloak 25 OAuth2/OIDC JWT RBAC matrix, salted phone hash. |
| **09** | [`09_frontend_pwa_and_ui_architecture.md`](file:///c:/ambulance/docs-src/09_frontend_pwa_and_ui_architecture.md) | **Frontend Architecture & PWA Specifications** | Dispatcher console, Paramedic crew mobile PWA responsive layout (smartphone & desktop), 5-phase mission stepper, clinical vitals panel, ED board, `demo-bridge.js`. |
| **10** | [`10_verification_testing_and_benchmarks.md`](file:///c:/ambulance/docs-src/10_verification_testing_and_benchmarks.md) | **Verification, Testing & Performance Benchmarks** | `jqwik` property-based testing (scorer monotonicity, concurrency invariance), Monte Carlo simulator with Common Random Numbers (CRN), latency & throughput benchmarks. |
| **11** | [`11_production_deployment_and_ops_manual.md`](file:///c:/ambulance/docs-src/11_production_deployment_and_ops_manual.md) | **Production Deployment & Operations Manual** | Hardware sizing, Ubuntu 24.04 setup, Docker Compose deployment, environment secrets, Prometheus & Grafana observability, automated backup & point-in-time recovery runbooks. |

---

## Summary of All 14 UML Diagrams in Chapter 5

1. **UML Model 1: System Use Case Diagram** — Primary actors (Caller, Dispatcher, Paramedic Crew, Hospital ED Doctor, Auditor, Admin) and operational use cases.
2. **UML Model 2: Package & Module Hierarchy Diagram** — Maven multi-module dependency tree enforcing pure Java 21 boundaries in `common` and `simulator`.
3. **UML Model 3: System Component Diagram** — Interconnections between the 8 Spring Boot microservices, Nginx, Spring Cloud Gateway, PostgreSQL PostGIS, Apache Kafka, Redis, and Keycloak.
4. **UML Model 4: Class Diagram (Core Domain Model)** — Core entity models (`AmbulanceUnit`, `Incident`, `Location`, `Hospital`, `DispatchScorer`, `CoverageModel`, `DestinationRanker`).
5. **UML Model 5: Class Diagram (Dispatch Service & Persistence)** — `DispatchService`, `AmbulanceUnitEntity`, `AssignmentEntity`, `OutboxEventEntity`, repositories, and controllers.
6. **UML Model 6: Sequence Diagram 1 (Incident Intake & Triage)** — Call intake, salted phone hashing, deterministic MPDS clinical triage, and outbox emission.
7. **UML Model 7: Sequence Diagram 2 (Candidate Scoring & Atomic Reservation)** — Routing matrix query, `DispatchScorer` evaluation, atomic conditional SQL update (`status = 'AVAILABLE'`), and conflict handling.
8. **UML Model 8: Sequence Diagram 3 (High-Frequency GPS Ingestion)** — Vehicle telematics streaming, Redis Geo spatial indexing, and Kafka publication.
9. **UML Model 9: Sequence Diagram 4 (Hospital Destination Ranking & Pre-Arrival Handover)** — Field triage, `DestinationRanker` query, pre-arrival alert creation, and AlertHub SSE transmission to the hospital trauma team.
10. **UML Model 10: State Machine Diagram 1 (Ambulance Unit Lifecycle)** — Operational state transitions: `AVAILABLE` -> `ASSIGNED` -> `EN_ROUTE` -> `ON_SCENE` -> `TRANSPORTING` -> `AT_HOSPITAL` -> `CLEARING` -> `AVAILABLE`.
11. **UML Model 11: State Machine Diagram 2 (Incident Emergency Lifecycle)** — Lifecycle states: `REPORTED` -> `TRIAGED` -> `DISPATCHED` -> `ON_SCENE` -> `TRANSPORTING` -> `RESOLVED` / `CANCELLED`.
12. **UML Model 12: Activity Diagram (End-to-End Clinical Dispatch Workflow)** — End-to-end flowchart from emergency call to hospital trauma bed handover.
13. **UML Model 13: Deployment Diagram (Production Cloud Infrastructure)** — Cloud VPS topology, Docker bridge network `h8-net`, persistent volumes, TLS termination, and monitoring.
14. **UML Model 14: Entity-Relationship (ER) Diagram (Relational Schema)** — Relational tables, foreign keys, PostGIS geometries, transactional outbox, and audit ledger.

---

## Compiled Master Deliverables

You can access and compile the entire documentation suite in two master formats:

1. **Master Markdown Document**:  
   📄 [`docs-src/H8_EMS_COMPLETE_SYSTEM_DOSSIER.md`](file:///c:/ambulance/docs-src/H8_EMS_COMPLETE_SYSTEM_DOSSIER.md)  
   *(12,313 words, fully formatted with all diagrams, tables, and code snippets)*

2. **Master Printable HTML Document**:  
   🌐 [`docs-src/H8_EMS_COMPLETE_SYSTEM_DOSSIER.html`](file:///c:/ambulance/docs-src/H8_EMS_COMPLETE_SYSTEM_DOSSIER.html)  
   *(Printable, styled executive edition with dynamic Mermaid.js rendering, page break formatting, and professional typography)*

### How to Export to a 60–70 Page PDF:
1. Double-click or open [`docs-src/H8_EMS_COMPLETE_SYSTEM_DOSSIER.html`](file:///c:/ambulance/docs-src/H8_EMS_COMPLETE_SYSTEM_DOSSIER.html) in Google Chrome, Microsoft Edge, or Firefox.
2. Press **`Ctrl + P`** (Print).
3. Select Destination: **"Save as PDF"**.
4. Set Paper size: **A4**, Margins: **Default**, Options: Check **"Background graphics"**.
5. Click **Save** to generate your publication-ready 60 to 70 page PDF document!
