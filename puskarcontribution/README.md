# H8 EMS — Security, Role-Based Access Control (RBAC) & HIPAA Phone Anonymizer
### Module Author: **Pushkar** (Security & Authorization Engineer)

---

## 1. Project & Module Overview
This repository contains the **Security, Authentication, and Privacy Gateway** of the H8 Emergency Medical Services (EMS) Platform, designed and implemented by **Pushkar**.

In an emergency medical platform handling real-time city dispatches and patient telephone calls, unauthorized access or patient data leakage constitutes a critical compliance violation. This module enforces strict **Role-Based Access Control (RBAC)** across dispatchers, admins, paramedic crews, and hospital staff, while guaranteeing **HIPAA & GDPR privacy** through salted cryptographic hashing.

```
                      [ Incoming User / Request ]
                                   │
                                   ▼
                   [ Tactical Authentication Gate ]
                 Verifies Passcode / Credentials
                                   │
                                   ▼
                    [ Cryptographic Bearer Token ]
                                   │
       ┌───────────────────────────┼───────────────────────────┐
       ▼                           ▼                           ▼
 [ Tactical Admin ]        [ 911 Dispatcher ]        [ Paramedic Crew ]
  Full Permissions          Intake & Ranking           Mission Stepper
       │                           │                           │
       └───────────────────────────┼───────────────────────────┘
                                   ▼
                    [ Salted Phone Anonymizer ]
                     Raw 911 Number ──> SHA-256
                     Protected Caller Hash (#3F9A12)
```

---

## 2. Key Contributions by Pushkar

### A. Tactical Authentication Gate & RBAC (`src/security/auth-manager.js`, `src/security/rbac-policy.json`)
- Designed the multi-tier role hierarchy:
  - `ADMIN`: Full tactical override, corridor control, audit log inspection.
  - `DISPATCHER`: Incident intake, candidate ranking, ambulance unit dispatch.
  - `CREW`: Unit authentication, GPS telemetry broadcast, 5-stage mission stepper.
  - `HOSPITAL_STAFF`: Resuscitation bay allocation, dynamic hospital diversion.
- Implemented tamper-evident session token issuance (`h8-auth-token-<payload>`) with expiration tracking.

### B. HIPAA & GDPR 911 Caller Phone Hasher (`src/security/salted-phone-hasher.js`)
- Solved the privacy dilemma in emergency medical systems: Caller phone numbers cannot be stored in plaintext in dispatch logs.
- Engineered a **Salted SHA-256 / PBKDF2 Anonymizer** converting raw phone numbers into deterministic, non-reversible hashes (`CALLER-#F48A3B`).
- Preserves the ability to link repeat emergency callers without revealing Personal Identifiable Information (PII).

### C. Spring Cloud API Gateway Security Filters (`src/api-gateway/`)
- Pre-routing authentication filter intercepting all HTTP & WebSocket traffic.
- Validates bearer tokens before forwarding calls to downstream microservices (`dispatch-service`, `hospital-service`).

### D. Audit Logging Microservice (`src/audit-service/`)
- Generates an immutable, timestamped audit log of every login attempt, dispatch action, and security override.

---

## 3. Technology Stack
- **Security & Cryptography**: WebCrypto API (SHA-256, PBKDF2), HMAC, Salted Hashes
- **Access Control**: Role-Based Access Control (RBAC), Bearer Tokens
- **Gateway & Audit**: Spring Cloud Gateway, Spring Boot Security Filters, Java 17
- **Testbed UI**: Standalone interactive HTML5/CSS3 Security Console

---

## 4. Directory Structure
```
02_Pushkar_Security_Auth/
├── README.md                          <-- You are here
├── run_module.bat                     <-- 1-Click runner for Pushkar's module
└── src/
    ├── api-gateway/                   <-- Java Spring Cloud API Gateway with Auth Filters
    │   ├── pom.xml
    │   └── src/main/java/com/h8/ems/gateway/
    ├── audit-service/                 <-- Java Spring Boot Audit Logging microservice
    │   ├── pom.xml
    │   └── src/main/java/com/h8/ems/audit/
    ├── security/
    │   ├── auth-manager.js            <-- Core RBAC & Bearer Token Controller
    │   ├── salted-phone-hasher.js     <-- HIPAA Salted Phone Anonymizer
    │   └── rbac-policy.json           <-- Security Policy & Permissions Matrix
    └── auth-ui/
        └── login-demo.html            <-- Interactive Security & Auth Testbed
```

---

## 5. How to Run & Test Pushkar's Module

### Option 1: Interactive Security Testbed (1-Click Run)
1. Double-click `run_module.bat` or run:
   ```cmd
   python -m http.server 8082 --directory src/auth-ui
   ```
2. Open your browser at:
   ```
   http://localhost:8082/login-demo.html
   ```
3. Test Authentication:
   - Enter `admin` / `admin123` $\rightarrow$ Observe `200 AUTH_GRANTED` with Admin Token.
   - Enter `baduser` / `wrongpwd` $\rightarrow$ Observe `401 AUTH_DENIED`.
4. Test Phone Hasher:
   - Enter any phone number (e.g. `+91 98290 12345`) $\rightarrow$ Observe instant Salted SHA-256 Digest and masked alias (`CALLER-#...`).

### Option 2: Java Spring API Gateway
```cmd
cd src\api-gateway
mvn spring-boot:run
```

---

## 6. Viva & Teacher Q&A Preparation (Questions Pushkar Can Answer)

**Q1: What was your specific role in this group project?**
> *Answer*: "I was the Security and Authorization Engineer. I developed the Role-Based Access Control (RBAC) engine, the Tactical Authentication Gate, the API Gateway pre-routing security filters, and the HIPAA-compliant salted telephone anonymizer."

**Q2: Why do you need salted hashing for telephone numbers? Why not simple encryption?**
> *Answer*: "With two-way encryption, encryption keys can be leaked or subpoenaed, compromising caller privacy. Salted one-way hashing (SHA-256 + secret salt) irreversibly masks the phone number, preventing rainbow table attacks while still allowing deterministic matching if the same caller calls back multiple times."

**Q3: How does your Role-Based Access Control (RBAC) work across services?**
> *Answer*: "We defined 4 distinct roles (Admin, Dispatcher, Crew, Hospital Staff) in a strict policy matrix. When a user logs in, they receive a signed bearer token containing their role and timestamp. The API Gateway validates this token before routing any request to downstream microservices."

**Q4: What happens if an unauthorized user tries to trigger a green-wave traffic corridor?**
> *Answer*: "The gateway inspects the token's permissions. Only the `ADMIN` role possesses the `ACTIVATE_GREEN_WAVE` permission. Any unauthorized attempt is rejected with HTTP 403 Forbidden and logged to the Audit Service."

---

## 7. How to Push This Module to Your Own GitHub
```bash
git init
git add .
git commit -m "Initial commit: Pushkar - Security, RBAC & HIPAA Anonymizer"
git branch -M main
git remote add origin https://github.com/<your-username>/ems-security-auth.git
git push -u origin main
```
