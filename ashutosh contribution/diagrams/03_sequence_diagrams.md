# UML Sequence Diagrams — EMS Platform
### Author: **Ashutosh (Ashu)** (Systems Design Architect & UML Specialist)

## Sequence 1: Incident Intake, Candidate Ranking & Unit Dispatch

```mermaid
sequenceDiagram
 autonumber
 actor Dispatcher as 911 Dispatcher (Manish UI)
 participant Hasher as Salted Phone Hasher (Pushkar)
 participant DispatcherSvc as Dispatch Service (Manish)
 participant Router as GraphHopper Routing (Manish)
 participant DB as PostGIS Supabase (Niraj)
 actor Crew as Paramedic Crew (Rahul PWA)

 Dispatcher->>Hasher: hashCallerPhone("+91 98290 12345")
 Hasher-->>Dispatcher: Return Masked Hash ("CALLER-#F48A")
 Dispatcher->>DispatcherSvc: POST /api/incidents (Coordinates, Delta Severity)
 
 DispatcherSvc->>DB: Query Available Units (ST_DWithin 10km)
 DB-->>DispatcherSvc: Return 4 Eligible Units
 
 loop For each candidate ambulance
 DispatcherSvc->>Router: GET /matrix/eta (UnitCoord, IncidentCoord)
 Router-->>DispatcherSvc: Precise Road ETA (seconds)
 DispatcherSvc->>DispatcherSvc: Compute S = 0.5·ETA + 0.3·Fit + 0.2·BedCap
 end

 DispatcherSvc-->>Dispatcher: Return Ranked Candidates (Top: AMB-01 ALS)
 Dispatcher->>DispatcherSvc: POST /api/dispatch/assign (UnitId, IncidentId)
 DispatcherSvc->>DB: INSERT dispatches & UPDATE unit.status='DISPATCHED'
 DispatcherSvc-->>Crew: Push Audio/Visual Dispatch Alert (WebSocket)
 Crew-->>DispatcherSvc: ACK 200 (Mission Accepted)
```

## Sequence 2: En Route, GPS Streaming & Hospital Trauma Handover

```mermaid
sequenceDiagram
 autonumber
 actor Crew as Paramedic Crew (Rahul PWA)
 participant Telemetry as Telemetry Streamer (Rahul)
 participant Cloud as Supabase Realtime (Niraj)
 actor Dispatcher as Dispatcher Map (Manish UI)
 actor Hospital as Hospital ED Board (Niraj UI)

 loop 1Hz Telemetry Loop
 Crew->>Telemetry: HTML5 watchPosition()
 Telemetry->>Cloud: Broadcast GPS (Lat, Lon, Speed, Bearing)
 Cloud-->>Dispatcher: Re-render vehicle marker on Leaflet GIS
 end

 Note over Crew: Paramedic advances to Step 3: Transporting
 Crew->>Hospital: Transmit Pre-Arrival Trauma Vitals (ETA: 4 min)
 Hospital->>Hospital: Allocate Resuscitation Bay #2 (Red Trauma)
 
 Note over Crew: Ambulance arrives at Emergency Bay
 Crew->>Crew: Tap Step 4: "Handover Complete"
 Crew->>Cloud: UPDATE dispatch.status='COMPLETED', unit.status='AVAILABLE'
 Cloud-->>Dispatcher: Ambulance restored to Available fleet
 Cloud-->>Hospital: Resus Bay updated to active patient care
```
