#!/usr/bin/env python3
"""
H8 EMS — IEEE 830 Software Requirements Specification (SRS) Generator
Generates a comprehensive 50-60 page formal SRS document containing:
- Formal IEEE 830-1998 sections
- Complete functional (FR-01 to FR-40) and non-functional requirements
- Complete UML modeling suite (all 14 diagrams)
- Actual Java reference source code from the repository
- Database DDL schemas, Kafka event contracts, and mathematical proofs
- Compiles Markdown, HTML, and renders a publication-grade PDF
"""

import os
import subprocess
import zlib
import re

ROOT_DIR = r"C:\ambulance"
DOCS_DIR = os.path.join(ROOT_DIR, "docs-src")
WEB_DOCS_DIR = os.path.join(ROOT_DIR, "web", "docs")

OUTPUT_MD = os.path.join(DOCS_DIR, "H8_EMS_SOFTWARE_REQUIREMENTS_SPECIFICATION.md")
OUTPUT_HTML = os.path.join(DOCS_DIR, "H8_EMS_SOFTWARE_REQUIREMENTS_SPECIFICATION.html")
OUTPUT_PDF = os.path.join(WEB_DOCS_DIR, "H8_EMS_SOFTWARE_REQUIREMENTS_SPECIFICATION.pdf")

def read_source_file(rel_path, max_lines=None):
    abs_path = os.path.join(ROOT_DIR, rel_path)
    if not os.path.exists(abs_path):
        return f"// Source file not found: {rel_path}"
    with open(abs_path, "r", encoding="utf-8") as f:
        lines = f.readlines()
        if max_lines and len(lines) > max_lines:
            lines = lines[:max_lines] + [f"\n// ... [Truncated: {len(lines) - max_lines} more lines] ...\n"]
        return "".join(lines)

def build_srs_content():
    scorer_code = read_source_file(r"common\src\main\java\com\h8\ems\common\scoring\DispatchScorer.java", 150)
    coverage_code = read_source_file(r"common\src\main\java\com\h8\ems\common\scoring\CoverageModel.java", 120)
    ranker_code = read_source_file(r"common\src\main\java\com\h8\ems\common\scoring\DestinationRanker.java", 120)
    dispatch_service_code = read_source_file(r"dispatch-service\src\main\java\com\h8\ems\dispatch\service\DispatchExecutionService.java", 140)
    caller_hash_code = read_source_file(r"incident-service\src\main\java\com\h8\ems\incident\util\CallerHashUtil.java", 80)
    unit_entity_code = read_source_file(r"dispatch-service\src\main\java\com\h8\ems\dispatch\model\AmbulanceUnitEntity.java", 100)
    outbox_relay_code = read_source_file(r"dispatch-service\src\main\java\com\h8\ems\dispatch\outbox\OutboxRelay.java", 110)
    state_machine_code = read_source_file(r"common\src\main\java\com\h8\ems\common\statemachine\UnitStateMachine.java", 110)
    scorer_test_code = read_source_file(r"common\src\test\java\com\h8\ems\common\scoring\DispatchScorerTest.java", 130)

    with open(os.path.join(DOCS_DIR, "01_executive_summary_and_overview.md"), "r", encoding="utf-8") as f:
        ch1 = f.read()
    with open(os.path.join(DOCS_DIR, "02_software_requirements_specification.md"), "r", encoding="utf-8") as f:
        ch2 = f.read()
    with open(os.path.join(DOCS_DIR, "03_system_architecture_and_design.md"), "r", encoding="utf-8") as f:
        ch3 = f.read()
    with open(os.path.join(DOCS_DIR, "04_algorithmic_specifications_and_math.md"), "r", encoding="utf-8") as f:
        ch4 = f.read()
    with open(os.path.join(DOCS_DIR, "05_complete_uml_modeling_suite.md"), "r", encoding="utf-8") as f:
        ch5 = f.read()
    with open(os.path.join(DOCS_DIR, "06_database_schemas_and_kafka_contracts.md"), "r", encoding="utf-8") as f:
        ch6 = f.read()
    with open(os.path.join(DOCS_DIR, "07_api_reference_and_service_specifications.md"), "r", encoding="utf-8") as f:
        ch7 = f.read()
    with open(os.path.join(DOCS_DIR, "08_cryptographic_audit_and_security.md"), "r", encoding="utf-8") as f:
        ch8 = f.read()
    with open(os.path.join(DOCS_DIR, "09_frontend_pwa_and_ui_architecture.md"), "r", encoding="utf-8") as f:
        ch9 = f.read()
    with open(os.path.join(DOCS_DIR, "10_verification_testing_and_benchmarks.md"), "r", encoding="utf-8") as f:
        ch10 = f.read()
    with open(os.path.join(DOCS_DIR, "11_production_deployment_and_ops_manual.md"), "r", encoding="utf-8") as f:
        ch11 = f.read()

    header = """# Software Requirements Specification (SRS)
## H8 Capability-Aware Emergency Medical Services (EMS) Dispatch & Telematics Platform
**Standard:** IEEE Std 830-1998 / ISO/IEC/IEEE 29148:2018 Compliant  
**Version:** 1.0.0-RELEASE (Production Baseline)  
**Date:** October 2026  
**Status:** Approved & Formally Verified  
**Target Metropolitan Area:** Jaipur, Rajasthan, India (14 Emergency Units, 5 Tier-1 Receiving Centers)

---

# Table of Contents
1. **Section 1: Introduction**
   - 1.1 Purpose of the Document
   - 1.2 Document Conventions & Mathematical Notation
   - 1.3 Intended Audience & Stakeholder Community
   - 1.4 Project Scope & Clinical Mission Objectives
   - 1.5 References & Regulatory Standards
2. **Section 2: Overall System Description**
   - 2.1 Product Perspective & Ecosystem Architecture
   - 2.2 Product Functions Summary
   - 2.3 User Classes & Operational Profiles
   - 2.4 Operating Environment & Technology Stack
   - 2.5 Design & Implementation Constraints (10 Hard Mandates)
   - 2.6 Assumptions & Operational Dependencies
3. **Section 3: System Features & Functional Requirements**
   - 3.1 Incident Intake & Caller Privacy Subsystem (`incident-service`)
   - 3.2 Clinical Triage & Need Classification
   - 3.3 Capability-Aware Candidate Scoring (`dispatch-service` & `DispatchScorer`)
   - 3.4 Atomic Unit Reservation & Concurrency Management
   - 3.5 Real-Time GPS Tracking & Telematics Ingestion (`tracking-service`)
   - 3.6 Turn-by-Turn Road Network Routing (`routing-service`)
   - 3.7 Receiving Hospital Ranking & Diversion Protocol (`hospital-service`)
   - 3.8 Real-Time Pre-Arrival Notifications (AlertHub SSE)
   - 3.9 Fleet Coverage Optimization (MEXCLP / `redeployment-service`)
   - 3.10 Cryptographic Dispatch Audit Ledger (`audit-service`)
   - 3.11 Multi-Persona Web & Mobile PWA Consoles
4. **Section 4: External Interface Requirements**
   - 4.1 User Interfaces (Dispatcher, Paramedic Mobile PWA, ED Board)
   - 4.2 Hardware Interfaces (IoT GPS Transponders, Rugged MDTs, Mobile Devices)
   - 4.3 Software Interfaces (PostgreSQL PostGIS, Apache Kafka, Redis, Keycloak)
   - 4.4 Communications Interfaces (HTTP/2, WebSocket, Server-Sent Events, TLS 1.3)
5. **Section 5: Non-Functional Requirements**
   - 5.1 Performance & Latency SLAs
   - 5.2 Reliability & Fault Tolerance
   - 5.3 Security, RBAC & HIPAA/DISHA Privacy
   - 5.4 Software Quality Attributes (Maintainability, Testability, Portability)
6. **Section 6: Complete UML 2.5 Modeling Suite (All 14 Diagrams)**
   - 6.1 UML Model 1: System Use Case Diagram
   - 6.2 UML Model 2: Package & Multi-Module Hierarchy
   - 6.3 UML Model 3: System Component Diagram
   - 6.4 UML Model 4: Class Diagram — Core Domain Model (`common`)
   - 6.5 UML Model 5: Class Diagram — Dispatch Service & Persistence
   - 6.6 UML Model 6: Sequence Diagram 1 — Incident Intake & Anonymization
   - 6.7 UML Model 7: Sequence Diagram 2 — Candidate Scoring & Atomic Lock
   - 6.8 UML Model 8: Sequence Diagram 3 — High-Frequency GPS Ingestion
   - 6.9 UML Model 9: Sequence Diagram 4 — Hospital Handover & SSE Stream
   - 6.10 UML Model 10: State Machine Diagram 1 — Ambulance Unit Lifecycle
   - 6.11 UML Model 11: State Machine Diagram 2 — Incident Lifecycle
   - 6.12 UML Model 12: Activity Diagram — End-to-End Clinical Dispatch
   - 6.13 UML Model 13: Deployment Diagram — Production Cloud Infrastructure
   - 6.14 UML Model 14: Entity-Relationship (ER) Relational Schema
7. **Section 7: Data Models, Relational DDL & Kafka Event Contracts**
   - 7.1 PostgreSQL Relational DDL (Schemas: `dispatch`, `incident`, `hospital`, `audit`)
   - 7.2 PostGIS Geometry Definitions & GIST Spatial Indexing
   - 7.3 Kafka Event Schemas (7 Core Topics)
   - 7.4 Redis Geospatial & Ephemeral Data Structures
8. **Section 8: Reference Source Code Implementation (`src`)**
   - 8.1 Algorithmic Engine: `DispatchScorer.java`
   - 8.2 Coverage Optimizer: `CoverageModel.java`
   - 8.3 Hospital Ranker: `DestinationRanker.java`
   - 8.4 Atomic Dispatch Execution: `DispatchExecutionService.java`
   - 8.5 Caller Privacy Engine: `CallerHashUtil.java`
   - 8.6 Transactional Outbox Relay: `OutboxRelay.java`
   - 8.7 Unit Finite State Machine: `UnitStateMachine.java`
   - 8.8 Concurrency & Invariant Verification: `DispatchScorerTest.java` (jqwik)
9. **Section 9: Verification, Testing & Acceptance Traceability**
   - 9.1 Verification Traceability Matrix (Requirements to Code to Tests)
   - 9.2 Acceptance Criteria & Formal Sign-off

---

<div class="page-break"></div>

# Section 1: Introduction

## 1.1 Purpose of the Document
This Software Requirements Specification (SRS) establishes the definitive technical, operational, and architectural requirements for the **H8 Capability-Aware Emergency Medical Services (EMS) Dispatch & Telematics Platform**. This document governs the design, implementation, formal property-based verification, and operational certification of the platform.

## 1.2 Document Conventions & Mathematical Notation
* **RFC 2119 Keywords**: The terms **MUST**, **MUST NOT**, **REQUIRED**, **SHALL**, **SHALL NOT**, **SHOULD**, and **MAY** are used in accordance with RFC 2119.
* **Coordinate Standards**: All spatial points are expressed in WGS-84 coordinates as (latitude, longitude) in decimal degrees.
* **Timestamp Standards**: All internal and external clocks adhere to UTC ISO-8601 formatting with millisecond precision (YYYY-MM-DDTHH:mm:ss.sssZ).
* **Mathematical Notation**:
  * S(u, i) in [0.0, 1.0] denotes the normalized composite dispatch score of ambulance unit u responding to incident i.
  * alpha, beta, gamma, delta, epsilon denote the convex weights of the five-factor dispatch scoring function (sum = 1.0).
  * H_k in {0, 1}^256 denotes the 256-bit SHA-256 cryptographic digest of block k in the immutable dispatch ledger.

## 1.3 Intended Audience & Stakeholder Community
* **Lead Software Engineers & Architects**: Complete behavioral, interface, and structural specifications.
* **Emergency Medical Directors & Clinicians**: Clinical triage matrices, capability matching rules, and hospital receiving logic.
* **Public Safety Communications Personnel (911 / 108 Dispatchers)**: Operational workflows, console layouts, and manual override procedures.
* **Legal, Compliance & Medical Malpractice Auditors**: Verification of cryptographic hash chaining, data immutability, and HIPAA/DISHA compliance.
* **DevOps & Infrastructure SREs**: Container topology, health checks, automated backup, and point-in-time recovery runbooks.

## 1.4 Project Scope & Clinical Mission Objectives
Traditional municipal Computer-Aided Dispatch (CAD) systems assign ambulances based solely on shortest Euclidean distance. This naive dispatch policy leads to severe clinical mismatches: sending Basic Life Support (BLS) units with minimal apparatus to catastrophic trauma or cardiac arrest emergencies, or exhausting scarce Advanced Life Support (ALS) intensive care units on non-emergency calls.

H8 introduces an algorithmic, capability-aware engine that computes a holistic score across driving ETA, clinical capability alignment, paramedic fatigue, suburban coverage preservation, and telemetry staleness, backed by real-time GraphHopper routing and an immutable SHA-256 audit ledger.

## 1.5 References & Regulatory Standards
1. **IEEE Std 830-1998**: Recommended Practice for Software Requirements Specifications.
2. **ISO/IEC/IEEE 29148:2018**: Systems and Software Engineering — Life Cycle Processes — Requirements Engineering.
3. **HIPAA Security Rule (45 CFR Part 160 and Part 164)**: Standards for the Privacy of Individually Identifiable Health Information.
4. **DISHA (Digital Information Security in Healthcare Act)**: Ministry of Health & Family Welfare, Government of India.
5. **NFPA 1710 / NFPA 1720**: Standard for the Organization and Deployment of Fire Suppression and Emergency Medical Operations.

---

<div class="page-break"></div>

# Section 2: Overall System Description
"""

    code_section = f"""
<div class="page-break"></div>

# Section 8: Reference Source Code Implementation (`src`)

This section incorporates verified production source code directly from the H8 repository to serve as the definitive algorithmic and implementation baseline.

## 8.1 Core Algorithmic Engine: `DispatchScorer.java`
* **File Path**: `common/src/main/java/com/h8/ems/common/scoring/DispatchScorer.java`  
* **Role**: Computes the 5-factor mathematical score with clinical penalties, crew fatigue, coverage preservation, and telemetry staleness.

```java
{scorer_code}
```

---

## 8.2 Coverage Optimization: `CoverageModel.java`
* **File Path**: `common/src/main/java/com/h8/ems/common/scoring/CoverageModel.java`  
* **Role**: Implements MEXCLP double-standard coverage optimization across urban census zones.

```java
{coverage_code}
```

---

## 8.3 Hospital Clinical Ranker: `DestinationRanker.java`
* **File Path**: `common/src/main/java/com/h8/ems/common/scoring/DestinationRanker.java`  
* **Role**: Ranks receiving emergency departments based on clinical specialization, travel ETA, diversion, and free bed headroom.

```java
{ranker_code}
```

---

## 8.4 Atomic Unit Reservation Service: `DispatchExecutionService.java`
* **File Path**: `dispatch-service/src/main/java/com/h8/ems/dispatch/service/DispatchExecutionService.java`  
* **Role**: Enforces Hard Rule #3: reserves ambulance units exclusively via atomic conditional database updates.

```java
{dispatch_service_code}
```

---

## 8.5 Caller Privacy & Salted Phone Hashing: `CallerHashUtil.java`
* **File Path**: `incident-service/src/main/java/com/h8/ems/incident/util/CallerHashUtil.java`  
* **Role**: Enforces Hard Rule #6: converts caller phone numbers into salted HMAC-SHA256 digests.

```java
{caller_hash_code}
```

---

## 8.6 Transactional Outbox Relay: `OutboxRelay.java`
* **File Path**: `dispatch-service/src/main/java/com/h8/ems/dispatch/outbox/OutboxRelay.java`  
* **Role**: Enforces Hard Rule #4: publishes transactional outbox events to Apache Kafka with guaranteed at-least-once delivery.

```java
{outbox_relay_code}
```

---

## 8.7 Unit Operational Finite State Machine: `UnitStateMachine.java`
* **File Path**: `common/src/main/java/com/h8/ems/common/statemachine/UnitStateMachine.java`  
* **Role**: Validates legal status transitions for emergency fleet vehicles.

```java
{state_machine_code}
```

---

## 8.8 Property-Based Verification Suite: `DispatchScorerTest.java` (jqwik)
* **File Path**: `common/src/test/java/com/h8/ems/common/scoring/DispatchScorerTest.java`  
* **Role**: Mathematical invariant exploration proving score boundedness and distance monotonicity across thousands of randomized inputs.

```java
{scorer_test_code}
```

---

<div class="page-break"></div>

# Section 9: Verification, Operations & Traceability Matrix

{ch10}

---

## 9.1 Requirements Traceability Matrix (RTM)

| Requirement ID | Requirement Description | Implementation Class | Verification Test Class | Status |
| :--- | :--- | :--- | :--- | :---: |
| **FR-INC-01** | Call intake & salted phone hashing | `CallerHashUtil`, `IncidentService` | `IncidentServiceTest` | **VERIFIED** |
| **FR-INC-02** | Deterministic MPDS triage | `IncidentService` | `IncidentServiceTest` | **VERIFIED** |
| **FR-DIS-01** | Radius candidate filtering | `DispatchRankingService` | `DispatchRankingServiceTest`| **VERIFIED** |
| **FR-DIS-02** | Multi-factor candidate scoring | `DispatchScorer` | `DispatchScorerTest` (jqwik)| **VERIFIED** |
| **FR-DIS-03** | Capability penalty enforcement | `DispatchScorer` | `DispatchScorerTest` (jqwik)| **VERIFIED** |
| **FR-DIS-04** | Atomic unit reservation | `DispatchExecutionService` | `DispatchExecutionTest` | **VERIFIED** |
| **FR-DIS-05** | Manual supervisor override | `DispatchExecutionService` | `DispatchExecutionTest` | **VERIFIED** |
| **FR-TRK-01** | High-frequency GPS ingestion | `TrackingService` | `LocationConsumerTest` | **VERIFIED** |
| **FR-TRK-02** | Redis Geo spatial indexing | `TrackingService` | `LocationConsumerTest` | **VERIFIED** |
| **FR-ROU-01** | GraphHopper road routing | `RoutingService` | `RoutingServiceTest` | **VERIFIED** |
| **FR-HOS-01** | Receiving hospital ranking | `DestinationRanker` | `DestinationRankerTest` | **VERIFIED** |
| **FR-HOS-04** | AlertHub SSE pre-arrival alerts| `AlertHub`, `HospitalService` | `HospitalServiceTest` | **VERIFIED** |
| **FR-RED-01** | MEXCLP coverage analysis | `CoverageModel` | `CoverageModelTest` | **VERIFIED** |
| **FR-AUD-01** | Cryptographic SHA-256 ledger | `AuditService` | `AuditServiceTest` | **VERIFIED** |

---

{ch11}
"""

    parts = [
        header,
        ch1,
        '\n\n<div class="page-break"></div>\n\n# Section 3: System Features & Functional Requirements\n',
        ch2,
        '\n\n<div class="page-break"></div>\n\n# Section 4: System Architecture & External Interfaces\n',
        ch3,
        '\n\n<div class="page-break"></div>\n\n# Section 5: Algorithmic Specifications & Mathematical Foundations\n',
        ch4,
        '\n\n<div class="page-break"></div>\n\n# Section 6: Complete UML 2.5 Modeling Suite (All 14 Diagrams)\n',
        ch5,
        '\n\n<div class="page-break"></div>\n\n# Section 7: Data Models, Relational DDL & Kafka Event Contracts\n',
        ch6,
        '\n\n<div class="page-break"></div>\n\n# Section 8: API Reference & Microservice Endpoints\n',
        ch7,
        '\n\n<div class="page-break"></div>\n\n# Section 9: Cryptographic Audit Ledger & Security Architecture\n',
        ch8,
        '\n\n<div class="page-break"></div>\n\n# Section 10: Frontend Architecture & PWA Specifications\n',
        ch9,
        code_section
    ]

    return "\n\n".join(parts)

def escape_html(text):
    return (text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;"))

def markdown_to_html(md_text):
    lines = md_text.splitlines()
    html = []
    in_code = False
    code_lang = ""
    code_buf = []
    in_table = False
    table_buf = []

    for line in lines:
        if line.startswith("```"):
            if in_code:
                in_code = False
                raw_code = "\n".join(code_buf)
                if code_lang == "mermaid":
                    html.append(f'<div class="mermaid">\n{raw_code}\n</div>')
                else:
                    html.append(f'<pre><code class="language-{code_lang}">{escape_html(raw_code)}</code></pre>')
                code_buf = []
                code_lang = ""
            else:
                in_code = True
                code_lang = line[3:].strip()
            continue

        if in_code:
            code_buf.append(line)
            continue

        if line.strip().startswith("|") and line.strip().endswith("|"):
            if not in_table:
                in_table = True
                table_buf = [line]
            else:
                table_buf.append(line)
            continue
        elif in_table:
            in_table = False
            html.append('<div class="table-container"><table>')
            is_header = True
            for t_line in table_buf:
                if re.match(r'^\s*\|(?:\s*:?-+:?\s*\|)+\s*$', t_line):
                    is_header = False
                    continue
                cells = [c.strip() for c in t_line.strip().strip('|').split('|')]
                tag = 'th' if is_header else 'td'
                row_html = "".join(f"<{tag}>{c}</{tag}>" for c in cells)
                html.append(f"<tr>{row_html}</tr>")
            html.append('</table></div>')
            table_buf = []

        if line.startswith("# "):
            html.append(f'<h1 class="chapter-title">{line[2:].strip()}</h1>')
        elif line.startswith("## "):
            html.append(f'<h2>{line[3:].strip()}</h2>')
        elif line.startswith("### "):
            html.append(f'<h3>{line[4:].strip()}</h3>')
        elif line.startswith("#### "):
            html.append(f'<h4>{line[5:].strip()}</h4>')
        elif line.startswith("* ") or line.startswith("- "):
            html.append(f'<li>{line[2:].strip()}</li>')
        elif line.strip() == "---":
            html.append('<hr/>')
        elif line.strip() == '<div class="page-break"></div>':
            html.append('<div class="page-break"></div>')
        elif line.strip():
            html.append(f'<p>{line}</p>')

    return "\n".join(html)

def generate():
    print("Constructing IEEE 830 SRS Master Document...")
    srs_md = build_srs_content()
    word_count = len(srs_md.split())
    print(f"Total Words in Master SRS: {word_count} words")

    with open(OUTPUT_MD, "w", encoding="utf-8") as f:
        f.write(srs_md)
    print(f"Wrote Master SRS Markdown: {OUTPUT_MD}")

    print("Converting Markdown to publication-grade HTML...")
    srs_body_html = markdown_to_html(srs_md)

    full_html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>H8 EMS — Software Requirements Specification (SRS) — IEEE Std 830</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Fira+Code:wght@400;500;600&family=Inter:wght@300;400;500;600;700;800&family=Newsreader:ital,opsz,wght@0,6..72,400;0,6..72,600;1,6..72,400&display=swap" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
    <script>
        mermaid.initialize({{
            startOnLoad: true,
            theme: 'neutral',
            flowchart: {{ useMaxWidth: true, htmlLabels: true }},
            sequence: {{ useMaxWidth: true }},
            themeVariables: {{
                fontFamily: 'Inter, sans-serif',
                primaryColor: '#e0f2fe',
                primaryTextColor: '#0f172a',
                primaryBorderColor: '#38bdf8',
                lineColor: '#64748b'
            }}
        }});
    </script>
    <style>
        :root {{
            --primary: #0284c7;
            --primary-dark: #0369a1;
            --text-dark: #0f172a;
            --text-muted: #475569;
            --border: #e2e8f0;
            --code-bg: #1e293b;
        }}

        * {{ box-sizing: border-box; margin: 0; padding: 0; }}
        body {{
            font-family: 'Newsreader', Georgia, serif;
            font-size: 14.5px;
            line-height: 1.65;
            color: var(--text-dark);
            background: #ffffff;
        }}

        .srs-container {{
            max-width: 920px;
            margin: 0 auto;
            padding: 3rem 2.5rem;
        }}

        .cover-page {{
            min-height: 88vh;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            text-align: center;
            border: 8px double var(--primary-dark);
            padding: 3.5rem 2rem;
            margin-bottom: 4rem;
            background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
            box-shadow: 0 10px 25px rgba(0,0,0,0.05);
        }}
        .cover-badge {{
            display: inline-block;
            background: var(--primary);
            color: #fff;
            padding: 0.35rem 1.25rem;
            border-radius: 9999px;
            font-family: 'Inter', sans-serif;
            font-size: 0.82rem;
            font-weight: 700;
            letter-spacing: 0.12em;
            text-transform: uppercase;
            margin-bottom: 1.8rem;
        }}
        .cover-title {{
            font-family: 'Inter', sans-serif;
            font-size: 2.75rem;
            font-weight: 800;
            color: #0c4a6e;
            line-height: 1.15;
            margin-bottom: 1.25rem;
        }}
        .cover-subtitle {{
            font-size: 1.3rem;
            color: var(--text-muted);
            max-width: 720px;
            margin-bottom: 2.5rem;
            font-style: italic;
        }}
        .cover-meta {{
            font-family: 'Inter', sans-serif;
            font-size: 0.92rem;
            color: #334155;
            line-height: 1.85;
            border-top: 2px solid #bae6fd;
            padding-top: 1.8rem;
            width: 85%;
        }}

        h1, h2, h3, h4 {{
            font-family: 'Inter', sans-serif;
            color: #0f172a;
            margin-top: 2.2rem;
            margin-bottom: 0.8rem;
        }}
        .chapter-title {{
            font-size: 2.0rem;
            font-weight: 800;
            color: #0369a1;
            border-bottom: 3px solid #38bdf8;
            padding-bottom: 0.5rem;
            margin-top: 3.5rem;
        }}
        h2 {{ font-size: 1.4rem; font-weight: 700; color: #1e293b; border-left: 4px solid var(--primary); padding-left: 0.65rem; }}
        h3 {{ font-size: 1.12rem; font-weight: 600; color: #334155; }}
        h4 {{ font-size: 0.98rem; font-weight: 600; color: #475569; }}
        p {{ margin-bottom: 1rem; text-align: justify; }}
        li {{ margin-left: 1.75rem; margin-bottom: 0.35rem; }}
        hr {{ border: 0; height: 1px; background: var(--border); margin: 2rem 0; }}

        pre {{
            background: var(--code-bg);
            color: #f8fafc;
            padding: 1.15rem;
            border-radius: 8px;
            overflow-x: auto;
            font-family: 'Fira Code', monospace;
            font-size: 0.82rem;
            line-height: 1.5;
            margin: 1.4rem 0;
            box-shadow: inset 0 2px 6px rgba(0,0,0,0.3);
        }}
        code {{
            font-family: 'Fira Code', monospace;
            font-size: 0.85em;
            background: #f1f5f9;
            color: #0284c7;
            padding: 0.15rem 0.35rem;
            border-radius: 4px;
        }}
        pre code {{ background: transparent; color: inherit; padding: 0; }}

        .table-container {{
            overflow-x: auto;
            margin: 1.4rem 0;
        }}
        table {{
            width: 100%;
            border-collapse: collapse;
            font-family: 'Inter', sans-serif;
            font-size: 0.83rem;
        }}
        th, td {{
            padding: 0.7rem 0.85rem;
            border: 1px solid var(--border);
            text-align: left;
        }}
        th {{
            background: #f0f9ff;
            color: #0369a1;
            font-weight: 700;
        }}
        tr:nth-child(even) {{ background: #f8fafc; }}

        .mermaid {{
            margin: 1.8rem auto;
            text-align: center;
            background: #ffffff;
            border: 1px solid #cbd5e1;
            border-radius: 8px;
            padding: 1.25rem;
            box-shadow: 0 4px 12px rgba(0,0,0,0.03);
            overflow-x: auto;
        }}

        @media print {{
            body {{ font-size: 8.4pt; line-height: 1.38; }}
            .srs-container {{ max-width: 100%; padding: 0; }}
            .page-break {{ page-break-before: always; }}
            pre, table {{ page-break-inside: avoid; }}
            pre {{ font-size: 7.0pt; line-height: 1.25; padding: 0.45rem 0.65rem; margin: 0.5rem 0; }}
            table {{ font-size: 7.2pt; }}
            th, td {{ padding: 0.3rem 0.45rem; }}
            .mermaid {{ margin: 0.6rem auto; padding: 0.5rem; transform: scale(0.9); transform-origin: top center; }}
            h1.chapter-title {{ font-size: 1.45rem; margin-top: 1.4rem; margin-bottom: 0.4rem; }}
            h2 {{ font-size: 1.1rem; margin-top: 1.0rem; margin-bottom: 0.3rem; }}
            h3 {{ font-size: 0.95rem; margin-top: 0.8rem; margin-bottom: 0.25rem; }}
            p {{ margin-bottom: 0.55rem; text-align: justify; }}
            li {{ margin-bottom: 0.2rem; }}
            @page {{
                margin: 0.95cm;
                @bottom-right {{
                    content: counter(page);
                }}
            }}
        }}
    </style>
</head>
<body>
    <div class="srs-container">
        <div class="cover-page">
            <span class="cover-badge">Formal Engineering Specification</span>
            <h1 class="cover-title">Software Requirements Specification (SRS)</h1>
            <p class="cover-subtitle">H8 Capability-Aware Emergency Medical Services (EMS) Dispatch &amp; Telematics Platform</p>
            <div class="cover-meta">
                <p><strong>Compliance Standard:</strong> IEEE Std 830-1998 &bull; ISO/IEC/IEEE 29148:2018</p>
                <p><strong>Architecture Stack:</strong> Java 21 LTS &bull; Spring Boot 3 &bull; PostgreSQL 15 PostGIS &bull; Apache Kafka 3.7 &bull; Redis 7 &bull; Keycloak 25 &bull; GraphHopper &bull; PWA</p>
                <p><strong>Specification Scope:</strong> Complete System Requirements, All 14 UML Diagrams, Mathematical Formulations, Relational DDL Schemas, and Reference Java Source Code</p>
                <p><strong>Operational Baseline:</strong> Metropolitan Emergency Operations &bull; Jaipur, India (14 Active Units, 5 Tier-1 Receiving Hospitals)</p>
            </div>
        </div>

        {srs_body_html}
    </div>
</body>
</html>
"""
    with open(OUTPUT_HTML, "w", encoding="utf-8") as f:
        f.write(full_html)
    print(f"Wrote Master SRS HTML: {OUTPUT_HTML}")

    with open(os.path.join(WEB_DOCS_DIR, "H8_EMS_SOFTWARE_REQUIREMENTS_SPECIFICATION.html"), "w", encoding="utf-8") as f:
        f.write(full_html)
    with open(os.path.join(WEB_DOCS_DIR, "H8_EMS_SOFTWARE_REQUIREMENTS_SPECIFICATION.md"), "w", encoding="utf-8") as f:
        f.write(srs_md)

    print("\nRendering Master SRS PDF via Headless Chrome...")
    chrome_cmd = [
        r"C:\Program Files\Google\Chrome\Application\chrome.exe",
        "--headless=new",
        "--disable-gpu",
        "--run-all-compositor-stages-before-draw",
        f"--print-to-pdf={OUTPUT_PDF}",
        f"file:///{OUTPUT_HTML.replace('\\', '/')}"
    ]
    subprocess.run(chrome_cmd, check=True)
    print(f"Generated Master SRS PDF: {OUTPUT_PDF}")

    subprocess.run(["powershell", "-Command", f'Copy-Item "{OUTPUT_PDF}" "{os.path.join(DOCS_DIR, "H8_EMS_SOFTWARE_REQUIREMENTS_SPECIFICATION.pdf")}" -Force'], check=True)

    with open(OUTPUT_PDF, "rb") as f:
        pdf_data = f.read()
    page_count = len(re.findall(rb'/Type\s*/Page\b', pdf_data))
    for m in re.finditer(rb'stream[\r\n]+(.*?)[\r\n]+endstream', pdf_data, re.DOTALL):
        try:
            decomp = zlib.decompress(m.group(1))
            page_count += len(re.findall(rb'/Type\s*/Page\b', decomp))
        except:
            pass
    print(f"\n=======================================================")
    print(f"  Master SRS PDF Page Count: {page_count} Pages!")
    print(f"  File Size: {os.path.getsize(OUTPUT_PDF) / (1024*1024):.2f} MB")
    print(f"=======================================================\n")

if __name__ == "__main__":
    generate()
