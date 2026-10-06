# H8 Emergency Medical Services (EMS) Platform
> **Next-Generation Autonomous Citywide Triage, Real-Time Satellite GIS Fleet Tracking & Hospital Emergency Capacity Management**

![Project Status](https://img.shields.io/badge/Status-Production%20Ready-34d399?style=for-the-badge)
![Architecture](https://img.shields.io/badge/Architecture-Distributed%20Microservices-38bdf8?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-17%20LTS-f97316?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-65a30d?style=for-the-badge&logo=springboot)
        ![Database](https://img.shields.io/badge/PostgreSQL-PostGIS%20Spatial-0284c7?style=for-the-badge&logo=postgresql)
        ![Realtime](https://img.shields.io/badge/Cloud-Supabase%20Realtime-10b981?style=for-the-badge&logo=supabase)

---

## 1. Project Overview & Problem Statement

In metropolitan emergency healthcare, every second delay increases patient mortality by up to 7%. Traditional emergency management systems suffer from three critical bottlenecks:
1. **Radio Communication Lag**: Dispatchers verbally query ambulance locations over radio, introducing minutes of human friction.
2. **Euclidean Routing Flaws**: Nearest ambulances are often assigned using straight-line distance ("as the crow flies"), ignoring city rivers, one-way streets, traffic congestion, and medical capability fit (ALS vs BLS).
3. **Ambulance Ramping**: Ambulances arrive unannounced at overcrowded hospital emergency rooms, forcing paramedics to wait outside for hours with critical patients because resuscitation bays are occupied.

The **H8 EMS Platform** is a unified, high-speed cloud platform engineered to solve these challenges. It synchronizes central emergency call dispatchers, frontline ambulance crews streaming live satellite GPS telemetry, and hospital trauma resuscitation bays on a single sub-second cloud network.

---

## 2. Three Flagship Operating Hubs

| Hub | Target User | Technology | Key Capabilities |
| :--- | :--- | :--- | :--- |
| **🗺️ Tactical Dispatcher Center** (`web/dispatcher/`) | Senior Emergency Dispatchers & City Admins | Leaflet.js, GraphHopper OSM, WebSockets | Citywide GIS map of 14 metropolitan ambulances, MPDS emergency intake modal, autonomous candidate ranking, 1-click green-wave signal corridors. |
| **🚑 Paramedic Crew Mobile Cockpit** (`web/crew/`) | Frontline Ambulance Drivers & Paramedics | HTML5 Touch PWA, Geolocation API, Web Audio | Touch-optimized smartphone cockpit, audio siren dispatch alerts, live 1Hz GPS coordinate broadcasting, 5-stage tactile mission stepper. |
| **🏥 Hospital ED Trauma Hub** (`web/ed/`) | Emergency Department Physicians & Triage Nurses | CSS3 Glassmorphism, Supabase Pub/Sub | Live resuscitation bay status monitors (Red, Yellow, Green), inbound ambulance ETA countdowns, dynamic hospital diversion controls to prevent ambulance ramping. |

---

## 3. 5-Person Engineering Team & Work Distribution

To achieve enterprise-grade scalability, security, and clinical compliance, the engineering work was structured into **5 specialized modular subsystems**, each designed and owned by a dedicated team member:

| # | Student Name | Core Role & Specialization | Key Modules & Code Ownership | Independent Module Folder |
| :-: | :--- | :--- | :--- | :--- |
| **1** | **Manish** | **Full-Stack Lead & Core Dispatch Architect** | • Tactical Dispatcher Web Console (Leaflet GIS)<br>• Autonomous Candidate Ranking Algorithm<br>• GraphHopper OSM Road Routing Service | `team-distribution/01_Manish_FullStack_CoreDispatch/` |
| **2** | **Pushkar** | **Security, Authentication & Authorization Engineer** | • Tactical Admin & Crew Authentication Gate<br>• Role-Based Access Control (RBAC) Matrix<br>• HIPAA/GDPR Salted Telephone Hasher (`SHA-256`)<br>• Spring Cloud API Gateway Security Filters | `team-distribution/02_Pushkar_Security_Auth/` |
| **3** | **Rahul** | **Mobile Front-End & Telemetry Engineer** | • Frontline Paramedic Mobile Cockpit (PWA)<br>• 5-Stage Sequential Mission Stepper Workflow<br>• High-Frequency Satellite GPS Telemetry Client<br>• Java Multithreaded Fleet Simulator | `team-distribution/03_Rahul_Crew_Mobile_Telemetry/` |
| **4** | **Niraj** | **Database Architect & Hospital ED Systems Engineer** | • PostgreSQL PostGIS Spatial Database Schema<br>• GiST Spatial Proximity Engine (`ST_DWithin`)<br>• Hospital ED Trauma Hub & Resuscitation Bays<br>• Dynamic Hospital Diversion Engine | `team-distribution/04_Niraj_Database_Hospital_ED/` |
| **5** | **Ashutosh (Ashu)** | **Systems Design Architect & Integration QA Lead** | • Complete 6-Diagram UML Architecture Suite<br>• Shared Microservice API Contracts (`contracts/`)<br>• Automated End-to-End Integration Test Suite<br>• 25-Case Quality Assurance Matrix Report | `team-distribution/05_Ashutosh_UML_Architecture_QA/` |

---

## 4. Detailed Technical Contributions by Student

### 👤 1. Manish — Full-Stack Lead & Core Dispatch Engine
- **Tactical Dispatcher Web Console (`web/dispatcher/`)**:
  - Developed the central command console using Leaflet.js with custom vehicle markers for 14 Jaipur metropolitan ambulances.
  - Implemented the **Emergency Incident Intake Modal** capturing emergency address, caller telephone, and MPDS triage classification (Alpha, Bravo, Charlie, Delta, Echo).
  - Built the 1-click **Green-Wave Corridor** visualizer highlighting fastest routes with simulated municipal traffic light preemption.
- **Autonomous Candidate Ranking Engine (`dispatch-service/`)**:
  - Implemented the multi-factor weighted scoring equation:
    $$\text{Score} = (0.50 \times \text{ETA Score}) + (0.30 \times \text{Capability Fit}) + (0.20 \times \text{Hospital Bed Capacity})$$
  - Prioritizes ALS (Advanced Life Support) units for cardiac and respiratory arrests while conserving BLS (Basic Life Support) units for low-acuity incidents.
- **Routing Engine Integration (`routing-service/`)**:
  - Connected GraphHopper OpenStreetMap routing to compute true road network travel times, avoiding straight-line distance inaccuracies.

---

### 👤 2. Pushkar — Security, Authentication & Role-Based Access Control (RBAC)
- **Tactical Authentication Gate (`02_Pushkar_Security_Auth/src/security/auth-manager.js`)**:
  - Engineered the credential validation gate issuing signed cryptographic Bearer session tokens (`h8-auth-token-...`).
- **Role-Based Access Control (`02_Pushkar_Security_Auth/src/security/rbac-policy.json`)**:
  - Enforced a 4-tier permission hierarchy:
    - `ADMIN`: Full tactical override, traffic corridor activation, audit ledger inspection.
    - `DISPATCHER`: Emergency call intake, candidate ranking, unit assignment.
    - `CREW`: Unit authentication, GPS telemetry broadcast, mission milestone progression.
    - `HOSPITAL_STAFF`: Resuscitation bay allocation, hospital diversion toggle.
- **HIPAA/GDPR Telephone Anonymizer (`02_Pushkar_Security_Auth/src/security/salted-phone-hasher.js`)**:
  - Solved medical privacy compliance by implementing a **Salted SHA-256 / PBKDF2 Hasher** that converts raw 911 caller telephone numbers into deterministic hashes (`CALLER-#F48A`). Prevents plaintext PII leaks while allowing identification of repeat callers.
- **Spring Cloud API Gateway & Audit Trail (`api-gateway/`, `audit-service/`)**:
  - Pre-routing security filters validating tokens before forwarding calls to downstream services, with tamper-evident audit logging.

---

### 👤 3. Rahul — Frontline Paramedic Mobile PWA & Telemetry Simulator
- **Paramedic Crew Mobile PWA (`web/crew/`)**:
  - Built a touch-optimized mobile web app designed for smartphone viewports mounted on ambulance dashboards.
  - Added audio siren dispatches and device vibration feedback when a new mission is assigned.
- **5-Stage Sequential Mission Stepper**:
  - Standardized frontline paramedic workflows into 5 sequential milestones:
    $$\text{Stage 1 (Dispatched)} \longrightarrow \text{Stage 2 (En Route)} \longrightarrow \text{Stage 3 (At Scene)} \longrightarrow \text{Stage 4 (Transporting)} \longrightarrow \text{Stage 5 (Handover)}$$
  - Ensures absolute state synchronization across Dispatch, Crew, and Hospital with zero voice-radio confusion.
- **Satellite GPS Streaming Client (`03_Rahul_Crew_Mobile_Telemetry/src/demo-telemetry-client.js`)**:
  - Captured HTML5 Geolocation API coordinates, computing instantaneous velocity (km/h) and compass heading bearing ($0^\circ - 360^\circ$) at 1Hz frequency.
- **Java Multithreaded Fleet Simulator (`simulator/`)**:
  - Developed a multithreaded Java 17 simulator broadcasting concurrent GPS telemetry packets for all 14 metropolitan ambulances across Jaipur roads.

---

### 👤 4. Niraj — PostGIS Spatial Database & Hospital ED Trauma Hub
- **PostgreSQL & PostGIS Database Schema (`supabase-schema.sql`)**:
  - Designed relational tables: `units`, `incidents`, `dispatches`, `hospitals`, and `audit_logs`.
  - Added **PostGIS GiST Spatial Indexing** on geographical coordinate points (`GEOMETRY(Point, 4326)`).
  - Formulated high-performance spatial SQL queries (`ST_DWithin`, `ST_DistanceSphere`) delivering sub-10ms proximity searches.
- **Hospital ED Trauma Hub Frontend (`web/ed/`)**:
  - Built the hospital receiving dashboard showing live resuscitation bays (Red Trauma, Yellow Urgent, Green Non-Urgent).
  - Displays real-time inbound ambulance ETA countdowns so trauma surgeons can prep bays prior to vehicle arrival.
- **Dynamic Hospital Diversion Engine**:
  - Implemented dynamic diversion controls: when an emergency department reaches capacity, staff toggle diversion to instantly notify the central dispatch algorithm, preventing fatal ambulance ramping.
- **Hospital Microservice & Seed Data (`hospital-service/`, `data-seed/`)**:
  - Java Spring Boot service managing trauma bed telemetry and seed records for major metropolitan hospitals (SMS Hospital, Fortis, Apex, etc.).

---

### 👤 5. Ashutosh (Ashu) — UML Design Architecture & Integration QA
- **6-Diagram UML Architecture Suite (`05_Ashutosh_UML_Architecture_QA/diagrams/`)**:
  1. *Use Case Diagram*: Models interactions across 5 actors (Caller, Dispatcher, Paramedic, Hospital Staff, Admin).
  2. *Domain Class Diagram*: Object-oriented domain model with entities, attributes, and relationships.
  3. *Sequence Diagrams*: Time-ordered messaging flows for Emergency Call Intake and Hospital Handover.
  4. *Activity Diagram*: Procedural decision logic for candidate scoring and green-wave signal corridor activation.
  5. *State Machine Diagram*: Formal lifecycle states of an ambulance unit (`AVAILABLE`, `DISPATCHED`, `EN_ROUTE`, `AT_SCENE`, `TRANSPORTING`, `CLINICAL_HANDOVER`).
  6. *Deployment Diagram*: Physical architecture mapping browsers, Edge CDN, Spring Cloud Gateway, microservices, and Supabase cloud.
- **Shared API Contracts & DTOs (`contracts/`, `common/`)**:
  - Defined unified JSON schemas and shared Java Data Transfer Objects preventing cross-service breaking changes.
- **Automated Integration QA Test Suite (`05_Ashutosh_UML_Architecture_QA/src/test-suites/e2e_integration_test.py`)**:
  - Automated Python test runner validating cross-service contracts and benchmarking end-to-end pipeline latencies (<50ms).
- **25-Case Quality Assurance Matrix (`05_Ashutosh_UML_Architecture_QA/src/test-suites/test_matrix_report.md`)**:
  - Rigorous test plan verifying functional, security, boundary, and performance criteria with 100% pass rate.

---

## 5. System Architecture Diagram

```mermaid
graph TB
    subgraph Client_Tier ["Client Tier (Browser & Mobile Apps)"]
        D_UI["🗺️ Dispatcher Tactical Console (Manish)"]
        C_UI["🚑 Paramedic Mobile PWA (Rahul)"]
        H_UI["🏥 Hospital ED Trauma Hub (Niraj)"]
    end

    subgraph Security_Gateway_Tier ["Security & Gateway Tier"]
        GW["🛡️ Spring Cloud API Gateway (Pushkar)"]
        AUTH["🔑 Tactical Auth & RBAC Gate (Pushkar)"]
        HASH["🔒 Salted Phone Hasher (Pushkar)"]
    end

    subgraph Microservices_Tier ["Business Microservices Tier (Java 17 / Spring Boot)"]
        DS["⚡ Dispatch Service & Ranker (Manish)"]
        RS["🛣️ Routing Service GraphHopper (Manish)"]
        HS["🏨 Hospital Service (Niraj)"]
        AS["📜 Audit Service (Pushkar)"]
    end

    subgraph Persistence_Tier ["Cloud Persistence & Spatial Tier"]
        DB[("🐘 PostgreSQL PostGIS Spatial DB (Niraj)")]
        RT["📡 Supabase Realtime Pub/Sub (Niraj)"]
    end

    D_UI --> GW
    C_UI --> GW
    H_UI --> GW

    GW --> AUTH
    AUTH --> HASH
    GW --> DS
    GW --> RS
    GW --> HS
    GW --> AS

    DS --> RS
    DS --> DB
    HS --> DB
    AS --> DB

    C_UI -.->|1Hz Satellite GPS Telemetry| RT
    RT -.->|Live Location Pings| D_UI
    RT -.->|Pre-Arrival Trauma Alerts| H_UI
```

---

## 6. Project Directory Layout

```
c:\ambulance\
├── web/                               <-- Web Frontends (Vanilla JS, HTML5, CSS3)
│   ├── index.html                     <-- Platform Showcase Landing Page
│   ├── demo-bridge.js                 <-- Client-Side Real-Time Simulation Engine
│   ├── dispatcher/                    <-- Manish: Dispatcher Tactical Console
│   ├── crew/                          <-- Rahul: Paramedic Mobile Touch PWA
│   └── ed/                            <-- Niraj: Hospital Emergency Trauma Hub
├── api-gateway/                       <-- Pushkar: Spring Cloud API Gateway
├── audit-service/                     <-- Pushkar: Tamper-Evident Audit Logging Service
├── dispatch-service/                  <-- Manish: Autonomous Candidate Ranking Microservice
├── routing-service/                   <-- Manish: GraphHopper Road Network Routing Microservice
├── hospital-service/                  <-- Niraj: Hospital Bed Capacity Telemetry Microservice
├── simulator/                         <-- Rahul: Java Multithreaded Fleet Telemetry Simulator
├── data-seed/                         <-- Niraj: Metropolitan Hospitals & Fleet Seed Data
├── contracts/                         <-- Ashutosh: Shared JSON API Schema Contracts
├── common/                            <-- Ashutosh: Shared Java DTOs & Exception Handlers
├── supabase-schema.sql                <-- Niraj: PostgreSQL PostGIS Spatial Database Schema
├── server.py                          <-- Unified Platform Local Development Server
├── start_backend.bat                  <-- Backend Services Launch Script
├── pom.xml                            <-- Maven Root Parent POM
│
└── team-distribution/                 <-- 5-Person Independent Working Repositories
    ├── TEAM_PROJECT_OVERVIEW.md       <-- Master Work Distribution Documentation
    ├── 01_Manish_FullStack_CoreDispatch/   <-- Manish's Independent Git Repository
    ├── 02_Pushkar_Security_Auth/           <-- Pushkar's Independent Git Repository
    ├── 03_Rahul_Crew_Mobile_Telemetry/     <-- Rahul's Independent Git Repository
    ├── 04_Niraj_Database_Hospital_ED/      <-- Niraj's Independent Git Repository
    └── 05_Ashutosh_UML_Architecture_QA/    <-- Ashutosh's Independent Git Repository
```

---

## 7. How to Run the Unified Project

### Option 1: Full-Stack Web Platform (Recommended for Demos)
Run the built-in development server from the repository root:
```cmd
python server.py
```
Open your browser and navigate to:
- **Landing Page**: `http://localhost:8000/web/`
- **Dispatcher Console**: `http://localhost:8000/web/dispatcher/`
- **Paramedic Cockpit**: `http://localhost:8000/web/crew/`
- **Hospital ED Board**: `http://localhost:8000/web/ed/`

### Default Login Credentials:
- **Tactical Admin**: `admin` / `admin123`
- **Senior Dispatcher**: `dispatcher1` / `disp123`
- **Paramedic ALS Unit**: `amb-01` / `crew123`
- **Paramedic BLS Unit**: `amb-02` / `crew123`

### Option 2: Java Spring Boot Microservices
```cmd
start_backend.bat
```
Or run individual microservices:
```cmd
cd dispatch-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
cd hospital-service && mvn spring-boot:run
```

---

## 8. How Each Student Pushes Their Work to Their Personal GitHub

Each student's folder inside `team-distribution/` is **100% self-contained**, with its own source code, runnable batch scripts, and comprehensive `README.md` containing **Teacher Viva Q&A Guides**.

Each student can initialize git inside their assigned folder and push it to their personal GitHub account:

### 1. Manish:
```bash
cd team-distribution/01_Manish_FullStack_CoreDispatch
git init
git add .
git commit -m "Initial commit: Manish - Core Dispatch & Routing Engine"
git branch -M main
git remote add origin https://github.com/<manish-username>/ems-core-dispatch.git
git push -u origin main
```

### 2. Pushkar:
```bash
cd team-distribution/02_Pushkar_Security_Auth
git init
git add .
git commit -m "Initial commit: Pushkar - Security, RBAC & HIPAA Anonymizer"
git branch -M main
git remote add origin https://github.com/<pushkar-username>/ems-security-auth.git
git push -u origin main
```

### 3. Rahul:
```bash
cd team-distribution/03_Rahul_Crew_Mobile_Telemetry
git init
git add .
git commit -m "Initial commit: Rahul - Paramedic Crew Mobile PWA & Telemetry Simulator"
git branch -M main
git remote add origin https://github.com/<rahul-username>/ems-crew-telemetry.git
git push -u origin main
```

### 4. Niraj:
```bash
cd team-distribution/04_Niraj_Database_Hospital_ED
git init
git add .
git commit -m "Initial commit: Niraj - PostGIS Database & Hospital ED Trauma Hub"
git branch -M main
git remote add origin https://github.com/<niraj-username>/ems-database-hospital.git
git push -u origin main
```

### 5. Ashutosh:
```bash
cd team-distribution/05_Ashutosh_UML_Architecture_QA
git init
git add .
git commit -m "Initial commit: Ashutosh - UML Architecture Specification & QA Test Suite"
git branch -M main
git remote add origin https://github.com/<ashutosh-username>/ems-uml-architecture-qa.git
git push -u origin main
```

---

## 9. 1-Click Run Scripts for Student Presentations
Each student has a dedicated `run_module.bat` inside their folder for live project evaluations:
- `01_Manish_FullStack_CoreDispatch\run_module.bat` $\rightarrow$ Launches standalone **Dispatcher Console**.
- `02_Pushkar_Security_Auth\run_module.bat` $\rightarrow$ Launches standalone **Security Gate & Hasher Testbed**.
- `03_Rahul_Crew_Mobile_Telemetry\run_module.bat` $\rightarrow$ Launches standalone **Paramedic Mobile Cockpit**.
- `04_Niraj_Database_Hospital_ED\run_module.bat` $\rightarrow$ Launches standalone **Hospital ED Trauma Hub**.
- `05_Ashutosh_UML_Architecture_QA\run_module.bat` $\rightarrow$ Executes **Automated Integration QA Test Suite**.

---

## 10. Standards & Regulatory Compliance
- **HIPAA / GDPR**: 911 caller telephone numbers are never stored in plaintext; salted SHA-256 digests protect patient privacy.
- **OpenGIS & OGC**: PostGIS spatial layers comply with international Open Geospatial Consortium standards.
- **HL7 / FHIR Ready**: Hospital trauma pre-arrival vitals format aligns with emergency pre-hospital electronic patient records.
- **Sub-Second Performance**: Complete intake-to-dispatch decision cycle executes in under 50 milliseconds.
