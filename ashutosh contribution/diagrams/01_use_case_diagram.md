# UML Use Case Diagram — EMS Platform
### Author: **Ashutosh (Ashu)** (Systems Design Architect & UML Specialist)

```mermaid
graph TD
 %% Actors
 Caller(("Caller (Citizen)"))
 Dispatcher(("Tactical Dispatcher"))
 Crew(("Paramedic Crew"))
 Hospital(("Hospital ED Staff"))
 Admin(("Tactical Admin"))

 %% Subsystem Boundary
 subgraph H8_EMS_Platform ["EMS Autonomous Orchestration Platform"]
 UC1(["Report Emergency (E-911 Call)"])
 UC2(["Intake Call & Mask Phone (HIPAA)"])
 UC3(["Autonomous Candidate Ranking"])
 UC4(["Assign Best-Fit Ambulance"])
 UC5(["Activate Green-Wave Traffic Corridor"])
 UC6(["Receive Dispatch Alert on Mobile PWA"])
 UC7(["Stream Live Satellite GPS Telemetry"])
 UC8(["Update 5-Stage Mission Stepper"])
 UC9(["Transmit Pre-Arrival Trauma Vitals"])
 UC10(["Allocate Resuscitation Bay"])
 UC11(["Toggle Hospital Diversion"])
 UC12(["Inspect Tamper-Evident Audit Ledger"])
 end

 %% Relationships
 Caller --> UC1
 Dispatcher --> UC2
 Dispatcher --> UC3
 Dispatcher --> UC4
 Dispatcher --> UC5
 
 Crew --> UC6
 Crew --> UC7
 Crew --> UC8
 Crew --> UC9

 Hospital --> UC10
 Hospital --> UC11

 Admin --> UC5
 Admin --> UC12

 UC2 -.->|includes| UC3
 UC3 -.->|includes| UC4
 UC4 -.->|notifies| UC6
 UC9 -.->|alerts| UC10
```

### Use Case Description Summary
1. **Intake Call & Mask Phone (UC2)**: Handled by Dispatcher with Pushkar's salted phone hasher.
2. **Autonomous Candidate Ranking (UC3)**: Handled by Manish's dispatch scoring algorithm.
3. **Stream Telemetry & 5-Step Stepper (UC7, UC8)**: Handled by Rahul's mobile PWA.
4. **Resus Bay & Hospital Diversion (UC10, UC11)**: Handled by Niraj's Hospital ED Hub.
5. **Inspect Audit Ledger (UC12)**: Handled by Admin through Pushkar's Audit Service.
