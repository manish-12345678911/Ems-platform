# UML Domain Class Diagram — EMS Platform
### Author: **Ashutosh (Ashu)** (Systems Design Architect & UML Specialist)

```mermaid
classDiagram
 class AmbulanceUnit {
 +UUID id
 +String callSign
 +UnitType type
 +UnitStatus status
 +Double latitude
 +Double longitude
 +Double speedKmH
 +Integer bearingDegrees
 +updateTelemetry(lat, lon, speed, bearing)
 +transitionState(newStatus)
 }

 class Incident {
 +UUID id
 +String incidentNumber
 +String saltedCallerHash
 +EmergencySeverity severity
 +Double latitude
 +Double longitude
 +String locationAddress
 +DateTime reportedAt
 +assignUnit(unitId)
 }

 class DispatchRecord {
 +UUID id
 +UUID incidentId
 +UUID unitId
 +UUID targetHospitalId
 +DispatchStatus status
 +Integer estimatedEtaSeconds
 +DateTime dispatchedAt
 +DateTime completedAt
 +completeHandover()
 }

 class Hospital {
 +UUID id
 +String name
 +Integer totalBeds
 +Integer availableBeds
 +Boolean diversionStatus
 +Double latitude
 +Double longitude
 +allocateResusBay()
 +toggleDiversion(status)
 }

 class TelemetryPacket {
 +String unitId
 +Double latitude
 +Double longitude
 +Double speedKmH
 +Integer bearing
 +Double accuracyMeters
 +DateTime timestamp
 +validateSchema()
 }

 class AuditLogEntry {
 +UUID id
 +String actorRole
 +String actionType
 +String resourceId
 +String sha256Fingerprint
 +DateTime timestamp
 }

 class CandidateRanker {
 +calculateScore(unit, incident, hospital): Double
 +rankAvailableUnits(incident): List~AmbulanceUnit~
 }

 AmbulanceUnit "1" -- "0..1" DispatchRecord : executes
 Incident "1" -- "1" DispatchRecord : generates
 Hospital "1" -- "0..*" DispatchRecord : receives
 AmbulanceUnit "1" -- "0..*" TelemetryPacket : broadcasts
 CandidateRanker ..> AmbulanceUnit : evaluates
 CandidateRanker ..> Incident : consumes
 DispatchRecord ..> AuditLogEntry : records
```

### Enumerations & Class Invariants
- `UnitType`: `ALS` (Advanced Life Support), `BLS` (Basic Life Support)
- `UnitStatus`: `AVAILABLE`, `DISPATCHED`, `EN_ROUTE`, `AT_SCENE`, `TRANSPORTING`, `OFFLINE`
- `EmergencySeverity`: `ECHO` (Immediate Catastrophic), `DELTA` (High Acuity), `CHARLIE` (Moderate), `BRAVO` (Low), `ALPHA` (Minor)
