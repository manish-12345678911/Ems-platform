# UML State Machine Diagram — Ambulance Unit Operational Lifecycle
### Author: **Ashutosh (Ashu)** (Systems Design Architect & UML Specialist)

```mermaid
stateDiagram-v2
    [*] --> AVAILABLE : Vehicle On-Duty & Registered

    AVAILABLE --> DISPATCHED : Incident Assigned by Dispatcher
    AVAILABLE --> OFFLINE : Shift End / Vehicle Maintenance

    DISPATCHED --> EN_ROUTE : Paramedic Crew Acknowledges Mission (Step 1)
    DISPATCHED --> AVAILABLE : Mission Cancelled by Dispatcher

    EN_ROUTE --> AT_SCENE : Ambulance Arrives at Patient Location (Step 2)
    
    AT_SCENE --> TRANSPORTING : Patient Stabilized, Rolling to Hospital (Step 3)
    AT_SCENE --> AVAILABLE : False Alarm / Treat and Release on Scene

    TRANSPORTING --> CLINICAL_HANDOVER : Ambulance Arrives at Hospital Resus Bay (Step 4)

    CLINICAL_HANDOVER --> AVAILABLE : Patient Care Transferred to ED Doctors (Step 5)
    
    OFFLINE --> AVAILABLE : Unit Re-enters Active Sector
```

### State Definitions
- **AVAILABLE**: Green status. Parked or patrolling sector base. Eligible for candidate ranking.
- **DISPATCHED**: Yellow pulse. Assignment transmitted. Awaiting crew departure.
- **EN_ROUTE**: Blue status. Rolling with emergency sirens. Turn-by-turn routing active.
- **AT_SCENE**: Purple status. On-scene medical stabilization and vital sign triage.
- **TRANSPORTING**: Coral emergency status. Carrying patient to designated trauma center.
- **CLINICAL_HANDOVER**: Transferring patient responsibility to emergency department doctors.
