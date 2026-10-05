# Chapter 9: Frontend Architecture & PWA Specifications

## 9.1 Multi-Persona Web Architecture
The presentation layer is structured as a **Progressive Web Application (PWA)** suite designed for responsive cross-device operations across desktop command centers, rugged vehicle tablets, and mobile smartphones:

```
+-----------------------------------------------------------------------------------------------+
|                            H8 UNIFIED FRONTEND PORTAL (PORT 8088)                              |
+-------------------------------+-------------------------------+-------------------------------+
| 1. Dispatcher Command Center  | 2. Paramedic Crew Mobile PWA  | 3. Hospital ED Tactical Board |
| Path: /dispatcher/            | Path: /crew/                  | Path: /ed/                    |
| - Leaflet live fleet map      | - Responsive mobile layout    | - 24/7 Bed capacity monitor   |
| - Candidate ranking matrix    | - 5-Phase interactive stepper | - Live AlertHub SSE stream    |
| - Real-time incident queue    | - Active clinical vitals card | - Instant diversion toggle    |
| - Algorithmic manual override | - Hospital pre-arrival alert  | - Trauma/Cath lab status      |
+-------------------------------+-------------------------------+-------------------------------+
|                      Client-Side Demo & Offline Engine: /demo-bridge.js                       |
|   BroadcastChannel Cross-Tab Sync  *  localStorage Persistence  *  Pure JS DispatchScorer     |
+-----------------------------------------------------------------------------------------------+
```

---

## 9.2 Paramedic Crew Mobile PWA (`/crew/`)

### 9.2.1 Responsive Design Philosophy (Mobile & Desktop Parity)
The Paramedic Crew Portal is engineered specifically for harsh, high-vibration ambulance operating environments. It operates seamlessly on:
1. **Desktop / In-Vehicle Mounted Terminals (Laptops & Heavy MDTs)**: Multi-column split layout showing navigation maps, route instructions, clinical vitals entry, and hospital handover simultaneously.
2. **Handheld Mobile Smartphones (iOS & Android)**: Specialized touch-first navigation tabs (`Console`, `Map`, `Vitals`, `EDs`, `Split`) with minimum 48px touch targets, high-contrast dark mode, and a sticky mobile quick-action bottom bar.

```
+-----------------------------------------------------------------------------------+
| [AMB-03 ALS]  [STANDBY PROTOCOL]                          (Battery 98%) (GPS Fix) |
+-----------------------------------------------------------------------------------+
|  STEPPER: (1) Assigned -> (2) En Route -> (3) On Scene -> (4) Transport -> (5) ED  |
+-----------------------------------------+-----------------------------------------+
| ACTIVE CLINICAL SUPPORT & VITALS PANEL  | LEAFLET REAL-TIME NAVIGATION MAP        |
| - Protocol: CARDIAC RESUSCITATION       | [Real-time GPS pin, polyline route      |
| - Blood Pressure: [ 120 ] / [ 80 ] mmHg |  turn-by-turn guidance to incident]     |
| - Heart Rate:     [ 84  ] bpm           |                                         |
| - SpO2 Telemetry: [ 98  ] %             |                                         |
| - GCS Score:      [ 15  ]               |                                         |
| - Cardiac Rhythm: [ Normal Sinus  v ]   |                                         |
+-----------------------------------------+-----------------------------------------+
| [Quick Action: ARRIVED ON SCENE]    [REQUEST DESTINATION RANKING]    [TRANSMIT ED]|
+-----------------------------------------------------------------------------------+
```

### 9.2.2 The 5-Phase Mission Lifecycle Stepper
The paramedic workflow is governed by an interactive five-phase mission lifecycle:
1. **Phase 1: ASSIGNED**: Ambulance receives emergency dispatch alert with patient chief complaint and incident coordinates.
2. **Phase 2: EN_ROUTE**: Siren active; real-time GPS telemetry tracks vehicle progress to incident scene.
3. **Phase 3: ON_SCENE**: Paramedics establish patient contact, initiate field stabilization, and record baseline vital signs.
4. **Phase 4: TRANSPORTING**: Patient loaded into ambulance; `DestinationRanker` queries optimal hospital, and pre-arrival notification is transmitted to the emergency department.
5. **Phase 5: AT_HOSPITAL**: Ambulance arrives at hospital ambulance bay; patient transferred to trauma team, and vehicle prepares for decontamination.

### 9.2.3 Persistent Clinical Support & Vitals Panel
Unlike naive portals that hide vital sign entry until hospital departure, H8 maintains an **always-visible clinical vitals matrix** featuring:
* Non-invasive Blood Pressure (Systolic / Diastolic).
* Continuous Heart Rate (BPM) with dynamic tachycardia/bradycardia color indicators.
* Pulse Oximetry ($SpO_2$) with hypoxia warning alerts ($<92\%$).
* Glasgow Coma Scale (GCS) calculator (Eye, Verbal, Motor).
* Electrocardiogram (ECG) rhythm classification selector.

---

## 9.3 Dispatcher Command Center (`/dispatcher/`)

The Dispatcher Command Center is the high-density tactical interface for emergency communications center operators:

### Key Operational Components:
1. **Real-Time Leaflet GIS Canvas**: Renders all 14 fleet ambulances with distinctive green (AVAILABLE), amber (EN_ROUTE/ON_SCENE), and red (TRANSPORTING) iconography. Receiving hospitals are rendered with live bed status tooltips.
2. **Dynamic Candidate Ranking Table**: Instantly visualizes the output of `DispatchScorer`, displaying each unit's distance, driving ETA, capability matching penalty, crew fatigue factor, and net dispatch score.
3. **Algorithmic Override Modal**: If an operator overrides the top-ranked unit, an imperative modal enforces entry of an audit justification reason before permitting the database lock to proceed.
4. **Suburban Coverage Strip**: Displays the live MEXCLP metropolitan coverage metric and alerts operators if suburban sectors drop below minimal emergency readiness thresholds.

---

## 9.4 Emergency Department Tactical Board (`/ed/`)

Designed for hospital triage nurses and emergency physicians:
* **Live Pre-Arrival Alerts**: As soon as an ambulance switches to `TRANSPORTING`, a real-time card appears via Server-Sent Events (SSE) detailing incoming patient acuity, clinical vitals, and countdown ETA.
* **Instant Diversion Control**: One-click toggling of hospital diversion status. If an emergency department experiences a sudden surge or trauma bay saturation, clicking `Declare Diversion` updates the central database and immediately notifies all active ambulances and dispatchers to reroute incoming transports.

---

## 9.5 The Client-Side Engine (`demo-bridge.js`)

To guarantee seamless execution in air-gapped environments, client demonstrations, and static CDN deployments (Netlify, Vercel, GitHub Pages):

### Architecture of `demo-bridge.js`:
* **Zero Backend Dependency**: Hooks directly into `window.fetch` and `window.EventSource`.
* **Pure JavaScript Dispatch Engine**: Implements the exact mathematical formulas of `DispatchScorer` and `haversine` distance directly in the browser.
* **Cross-Tab Synchronization**: Leverages the browser `BroadcastChannel('h8_demo_sync')` API and `localStorage`. When an operator dispatches AMB-03 on the Dispatcher console tab, the Crew PWA tab instantly updates to `ASSIGNED` in under 5 milliseconds with zero network requests.
