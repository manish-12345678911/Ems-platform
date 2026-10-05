# Phase 0–1 Completion Handoff — H8 EMS Platform

> **Purpose**: This file gives a new agent/model all context needed to continue from **Phase 2 (simulator)** onward without re-reading the full ARCHITECTURE.md or scanning every file. Read this first, then build.

---

## 1. Environment

| Item | Value |
|------|-------|
| OS | Windows 11, PowerShell |
| Java | JDK 27 at `C:\Program Files\Java\jdk-27` |
| Maven | 3.9.9 at `C:\tools\apache-maven-3.9.9` |
| Workspace | `c:\ambulance` |
| Maven run command | `$env:JAVA_HOME = "C:\Program Files\Java\jdk-27"; $env:PATH = "C:\tools\apache-maven-3.9.9\bin;$env:PATH"; mvn <args>` |
| Build verified | `mvn compile -pl common,contracts -am` → BUILD SUCCESS |
| Tests verified | `mvn test -pl common` → 50 tests, 0 failures |

---

## 2. Project Structure (what exists now)

```
c:\ambulance\
├── pom.xml                          # Parent POM (Java 21, Spring Boot 3.3.4 BOM)
├── AGENTS.md                        # Hard rules for agents (read this!)
├── ARCHITECTURE.md                  # Full architecture spec (803 lines)
├── docs/ARCHITECTURE.md             # Copy in docs/
├── docker-compose.yml               # Full infra + all services
├── .env.example                     # Secrets template
│
├── common/                          # ✅ COMPLETE — plain Java 21 library
│   ├── pom.xml
│   └── src/main/java/com/h8/ems/common/
│       ├── model/
│       │   ├── GeoPoint.java            # record(lat, lon) + distanceTo(Haversine)
│       │   ├── Severity.java            # CRITICAL(0.25), EMERGENCY(0.50), URGENT(0.75), LOW(1.0)
│       │   ├── ClinicalNeed.java        # TRAUMA, CARDIAC, STROKE, PEDIATRIC, BURN, GENERAL
│       │   ├── UnitType.java            # ALS, BLS
│       │   ├── UnitStatus.java          # 6 states + validTransitions() + canTransitionTo()
│       │   ├── IncidentStatus.java      # 8 states + validTransitions() + canTransitionTo()
│       │   ├── UnitSnapshot.java        # record(id, callSign, type, status, position, positionAt, shiftStart, homeStationId, homeStationLocation)
│       │   ├── IncidentSnapshot.java    # record(id, location, severity, need, requiresAls, status, receivedAt)
│       │   └── HospitalSnapshot.java    # record(id, name, location, capabilities, edBedsFree, icuBedsFree, ventilatorsFree, capacityUpdatedAt)
│       ├── statemachine/
│       │   ├── UnitStateMachine.java          # static check(from, to) throws IllegalStateTransitionException
│       │   └── IllegalStateTransitionException.java
│       ├── eta/
│       │   ├── EtaProvider.java               # interface: double etaSeconds(GeoPoint from, GeoPoint to, Instant at)
│       │   └── HaversineEta.java              # implements EtaProvider, 50 km/h base speed + time-of-day factor
│       └── scoring/
│           ├── ScorerParams.java              # record(etaWeight, capabilityWeight, fatigueWeight, coverageWeight, stalePenaltyWeight, ...) + defaults()
│           ├── DispatchScorer.java             # score(unit, incident, etaSec, now, availableUnits) → double; breakdown() → ScoreBreakdown record
│           ├── CoverageModel.java             # coverage(units), lossIfRemoved(unit, available), coverageIfMoved(units, unit, standby)
│           ├── DestinationRanker.java         # rank(incident, hospitals, transportEta, now, ttlMinutes) → List<RankedHospital>
│           └── RedeploymentPlanner.java       # plan(idleUnits, standbyPoints, PlannerLimits) → List<RedeployMove>
│
├── contracts/                       # ✅ COMPLETE — Kafka events + REST DTOs
│   ├── pom.xml                      # depends on: common
│   └── src/main/java/com/h8/ems/contracts/
│       ├── events/
│       │   ├── IncidentEvent.java         # record(eventId, type, incidentId, at, body, schemaVersion)
│       │   ├── UnitStatusEvent.java       # record(eventId, unitId, from, to, at, schemaVersion)
│       │   ├── LocationUpdate.java        # record(unitId, lat, lon, epochMs, speed, schemaVersion)
│       │   ├── DispatchDecision.java      # record(eventId, incidentId, unitId, chosenBy, List<RankedCandidate> ranked, at, schemaVersion)
│       │   ├── PreArrivalAlert.java       # record(eventId, incidentId, hospitalId, severity, need, requiresAls, etaSeconds, sentAt, schemaVersion)
│       │   └── AuditEvent.java            # record(eventId, kind, actor, payload, at, schemaVersion)
│       └── dto/
│           ├── CreateIncidentRequest.java # record(lat, lon, severity, need, requiresAls, callerHash)
│           ├── EtaRequest.java            # record(fromLat, fromLon, toLat, toLon)
│           └── EtaResponse.java           # record(etaSeconds, fallback)
│
├── simulator/                       # ⬅ PHASE 2 TARGET — placeholder only
│   ├── pom.xml                      # depends on: common (NOT Spring). Has shade plugin → fat JAR
│   └── src/main/java/com/h8/ems/simulator/Main.java  # placeholder println
│
├── data-seed/                       # placeholder
├── api-gateway/                     # placeholder app + config + Dockerfile
├── incident-service/                # placeholder app + config + Dockerfile + Flyway V1
├── dispatch-service/                # placeholder app + config + Dockerfile + Flyway V1
├── tracking-service/                # placeholder app + config + Dockerfile
├── routing-service/                 # placeholder app + config + Dockerfile
├── hospital-service/                # placeholder app + config + Dockerfile + Flyway V1
├── redeployment-service/            # placeholder app + config + Dockerfile + Flyway V1
├── audit-service/                   # placeholder app + config + Dockerfile + Flyway V1
├── web/                             # placeholder HTML pages (dispatcher, crew, ed)
├── ops/                             # prometheus.yml + keycloak/h8-realm.json
├── data/                            # (empty — OSM extract + zone grid go here)
└── experiments/                     # (empty — scenario YAML + results go here)
```

---

## 3. Hard Rules (from AGENTS.md — YOU MUST FOLLOW THESE)

1. `common` and `simulator` must NOT depend on Spring, JPA, Kafka or Redis. Plain Java 21 only.
2. DispatchScorer, CoverageModel, DestinationRanker live only in `common`. Import them, never copy.
3. A unit is reserved only by a conditional UPDATE (status = AVAILABLE). Never read-then-write.
4. Every Kafka consumer is idempotent (dedupe on eventId).
5. All unit positions and hospital capacities carry a timestamp and TTL. Stale data is penalised or ignored.
6. No real patient or caller data. Caller phone stored only as salted hash.
7. **Simulator runs are deterministic for a given seed. Use common random numbers across policies.**
8. Every new class gets a unit test.
9. Do not invent library APIs.
10. Never commit secrets.

---

## 4. Common Library API Reference (what the simulator imports)

### 4.1 GeoPoint
```java
// com.h8.ems.common.model.GeoPoint
record GeoPoint(double lat, double lon) {
    double distanceTo(GeoPoint other);     // km, Haversine
    double distanceToMeters(GeoPoint other);
}
```

### 4.2 Enums
```java
enum Severity    { CRITICAL(0.25), EMERGENCY(0.50), URGENT(0.75), LOW(1.0) }  // scaleFactor()
enum ClinicalNeed { TRAUMA, CARDIAC, STROKE, PEDIATRIC, BURN, GENERAL }
enum UnitType    { ALS, BLS }
enum UnitStatus  { AVAILABLE, DISPATCHED, ON_SCENE, TRANSPORTING, AT_HOSPITAL, OFFLINE }
    // .canTransitionTo(UnitStatus) → boolean
    // .validTransitions() → Set<UnitStatus>
enum IncidentStatus { RECEIVED, TRIAGED, DISPATCHED, ON_SCENE, TRANSPORTING, HANDED_OVER, CLOSED, CANCELLED }
    // same methods
```

### 4.3 Snapshots (JPA-free domain records)
```java
record UnitSnapshot(UUID id, String callSign, UnitType type, UnitStatus status,
                    GeoPoint position, Instant positionAt, Instant shiftStart,
                    UUID homeStationId, GeoPoint homeStationLocation) {
    boolean isPositionStale(Instant now, long ttlSeconds);
    double hoursOnShift(Instant now);
}

record IncidentSnapshot(UUID id, GeoPoint location, Severity severity,
                        ClinicalNeed need, boolean requiresAls,
                        IncidentStatus status, Instant receivedAt)

record HospitalSnapshot(UUID id, String name, GeoPoint location,
                        Set<ClinicalNeed> capabilities,
                        int edBedsFree, int icuBedsFree, int ventilatorsFree,
                        Instant capacityUpdatedAt) {
    boolean isCapacityStale(Instant now, int ttlMinutes);
    boolean supports(ClinicalNeed need);
    double estimatedWaitMinutes();
}
```

### 4.4 State Machine
```java
UnitStateMachine.check(UnitStatus from, UnitStatus to);   // throws IllegalStateTransitionException
UnitStateMachine.isValid(UnitStatus from, UnitStatus to);  // returns boolean
```

### 4.5 ETA
```java
interface EtaProvider {
    double etaSeconds(GeoPoint from, GeoPoint to, Instant at);
}

class HaversineEta implements EtaProvider  // 50 km/h + rush hour(0.7x) + night(1.3x)
```

### 4.6 DispatchScorer
```java
class DispatchScorer {
    DispatchScorer(ScorerParams params, CoverageModel coverageModel);
    DispatchScorer(CoverageModel coverageModel);  // uses ScorerParams.defaults()

    double score(UnitSnapshot unit, IncidentSnapshot incident,
                 double etaSeconds, Instant now, List<UnitSnapshot> availableUnits);

    ScoreBreakdown breakdown(UnitSnapshot unit, IncidentSnapshot incident,
                              double etaSeconds, Instant now, List<UnitSnapshot> availableUnits);

    record ScoreBreakdown(double totalScore, double etaSeconds,
                          double etaComponent, double capabilityComponent,
                          double fatigueComponent, double coverageComponent,
                          double stalenessComponent);
}

record ScorerParams(double etaWeight, double capabilityWeight, double fatigueWeight,
                    double coverageWeight, double stalePenaltyWeight,
                    double fatigueThresholdHours, double staleTtlSeconds,
                    double coverageRadiusKm) {
    static ScorerParams defaults(); // (0.40, 0.25, 0.10, 0.15, 0.10, 10.0, 90.0, 8.0)
}
```

### 4.7 CoverageModel
```java
class CoverageModel {
    CoverageModel(double coverageRadiusKm, List<GeoPoint> demandZoneCentroids);
    double coverage(List<UnitSnapshot> units);                               // [0,1]
    double lossIfRemoved(UnitSnapshot unit, List<UnitSnapshot> available);    // [0,1]
    double coverageIfMoved(List<UnitSnapshot> units, UnitSnapshot unit, GeoPoint standby);  // [0,1]
}
```

### 4.8 DestinationRanker
```java
class DestinationRanker {
    List<RankedHospital> rank(IncidentSnapshot incident, List<HospitalSnapshot> hospitals,
                               BiFunction<GeoPoint, GeoPoint, Double> transportEta,
                               Instant now, int ttlMinutes);

    record RankedHospital(HospitalSnapshot hospital, double score,
                          double transportEtaSeconds, double estimatedWaitMinutes,
                          boolean capacityStale);
}
```

### 4.9 RedeploymentPlanner
```java
class RedeploymentPlanner {
    RedeploymentPlanner(CoverageModel coverageModel);
    List<RedeployMove> plan(List<UnitSnapshot> idleUnits, List<GeoPoint> standbyPoints,
                             PlannerLimits limits);

    record RedeployMove(UUID unitId, String callSign,
                        GeoPoint currentPosition, GeoPoint target, double coverageGain);
    record PlannerLimits(int maxMoves, double minGain, int cooldownMinutes) {
        static PlannerLimits defaults();  // (3, 0.01, 10)
    }
}
```

---

## 5. Phase 2 Task: Simulator

### 5.1 What to build
All code goes in `simulator/src/main/java/com/h8/ems/simulator/`. Tests in `simulator/src/test/java/...`.

**Module rule**: simulator depends ONLY on `common`. No Spring, no JPA, no Kafka, no Redis. Plain Java 21 + SnakeYAML + Jackson + SLF4J (already in `simulator/pom.xml`).

### 5.2 Classes to implement (from ARCHITECTURE.md section 11)

| Class | Purpose |
|-------|---------|
| `Main` | CLI entry point: `--mode=batch|replay --scenarios=S1 --policies=B1,B2,P1 --reps=5 --seeds=42,43 --out=dir` |
| `ScenarioConfig` | YAML-loaded config: fleet size, fleet mix (ALS/BLS %), zone grid, demand profile, hospital setup |
| `DemandGenerator` | Non-homogeneous Poisson process via thinning, zone CDF weighted by demand_per_hour |
| `SimEvent` | Sealed interface: `IncidentArrived`, `UnitDispatched`, `UnitArrivedScene`, `UnitTransporting`, `UnitArrivedHospital`, `UnitAvailable` |
| `SimEngine` | PriorityQueue<SimEvent> driving the DES loop |
| `TravelModel` | Wraps EtaProvider (HaversineEta) + log-normal noise |
| `HospitalModel` | Occupancy tracking, handover time, alert effect |
| `SimState` | Mutable world state: units, incidents, hospitals, clock |
| `Policy` | Interface with implementations: B1 (nearest), B2 (nearest ALS-aware), P1 (DispatchScorer), P2 (P1 + hospital ranking), P3 (P2 + redeployment), plus ablation flags |
| `Metrics` | Collects per-incident: response time, on-scene time, transport time, handover delay, coverage snapshots |
| `MetricsAggregator` | Computes mean, median, p90, p95, within-target %, coverage over time |
| `ExperimentRunner` | Reads scenario YAML, runs all policies × seeds, writes CSV |

### 5.3 Critical design rules

1. **Common random numbers**: One incident stream per (scenario, seed), reused by every policy. Pre-sample per-incident random draws (scene time, noise, handover time) keyed by incident ID before policies run. A policy serving an incident later or with a different unit must NOT shift the random sequence for other incidents.

2. **Determinism**: Same seed → identical output. Use `java.util.Random` seeded deterministically. Verify with a test: run twice with same seed, assert files are byte-identical.

3. **Two modes**:
   - `batch`: Pure in-process, fast. No HTTP calls. Used for experiments E1–E5, E8.
   - `replay`: Same incident stream drives the live HTTP APIs via simulated GPS. For end-to-end demos (E6, E7). Can be a stub initially.

4. **Output**: One CSV row per incident per policy per seed. Columns: `scenario,policy,seed,incidentId,severity,need,receivedAt,dispatchedAt,arrivedSceneAt,transportStartedAt,arrivedHospitalAt,handedOverAt,responseTimeSec,unitCallSign,unitType,hospitalName`. So any statistic can be recomputed from raw data.

5. **Scenario files** go in `experiments/scenarios/*.yaml`. Create at least S1 (urban baseline: 20 units, 5 hospitals, high demand).

### 5.4 Available dependencies (already in simulator/pom.xml)

```xml
com.h8.ems:common           # all the scoring, coverage, state machine, ETA
org.yaml:snakeyaml           # YAML parsing
com.fasterxml.jackson.*      # JSON/YAML serialization
org.slf4j:slf4j-api          # logging
ch.qos.logback:logback-classic
org.junit.jupiter:junit-jupiter  (test)
net.jqwik:jqwik                  (test)
```

### 5.5 Acceptance criteria

- `java -jar simulator/target/simulator-1.0.0-SNAPSHOT.jar --mode=batch --scenarios=S1 --policies=B1,B2,P1 --reps=5 --out=experiments/results` produces CSVs
- Running twice with same seed gives identical files (determinism test must be in test suite)
- B1 response times are plausible (sanity: mean ~8–15 min for urban)

### 5.6 Build & test commands

```powershell
# Set environment
$env:JAVA_HOME = "C:\Program Files\Java\jdk-27"
$env:PATH = "C:\tools\apache-maven-3.9.9\bin;$env:PATH"

# Compile simulator (builds common first)
mvn compile -pl simulator -am

# Run simulator tests
mvn test -pl simulator

# Package fat JAR
mvn package -pl simulator -am -DskipTests

# Run the JAR
java -jar simulator/target/simulator-1.0.0-SNAPSHOT.jar --mode=batch --scenarios=S1 --policies=B1,B2,P1 --reps=2 --out=experiments/results
```

---

## 6. Architecture Corrections Already Applied

| # | Description | Applied In |
|---|-------------|-----------|
| 1 | `coverageIfMoved()` added | CoverageModel |
| 2 | Planner uses UnitSnapshot copies, not JPA entities | RedeploymentPlanner |
| 3 | JPA-free snapshots in common | All snapshot records |
| 6 | ALS/BLS scoring prevents BLS beating ALS for CRITICAL | DispatchScorer |
| 8 | Stale capacity penalty verified in DestinationRanker | DestinationRanker |
| 10 | HaversineEta returns real values (not stub 0) | HaversineEta |

### Corrections still to apply (in later phases):
| # | Description | Phase |
|---|-------------|-------|
| 4 | Drop manual version bumping in dispatch conditional UPDATE | Phase 4 |
| 5 | Make radius/limit configurable with rural fallback | Phase 4 (already in config) |
| 7 | Use Lua script for atomic Redis timestamp compare | Phase 4 |
| 9 | AlertHub: heartbeat, onError removal, Last-Event-ID | Phase 5 |

---

## 7. Remaining Phases After Phase 2

| Phase | Description | Depends on |
|-------|-------------|-----------|
| 3 | Data layer + incident-service + routing-service + outbox relay | contracts |
| 4 | Tracking-service + dispatch-service (conditional UPDATE, Redis GEO) | contracts, common |
| 5 | Hospital-service + redeployment-service + GraphHopper ETA | common, contracts |
| 6 | API gateway + Keycloak security + web pages (Leaflet, PWA, ED) | all services |
| 7 | Audit-service + Micrometer metrics + Grafana + fault injection | all services |
| 8 | Experiments E1–E8 + statistical analysis + paper | simulator + all services |

---

## 8. File Quick-Reference

| Need | File |
|------|------|
| Full architecture spec | `docs/ARCHITECTURE.md` (section 11 = simulator) |
| Hard rules | `AGENTS.md` |
| Parent POM with all versions | `pom.xml` |
| Simulator POM | `simulator/pom.xml` |
| GeoPoint & all models | `common/src/main/java/com/h8/ems/common/model/` |
| Scoring (DispatchScorer etc.) | `common/src/main/java/com/h8/ems/common/scoring/` |
| ETA interface + Haversine | `common/src/main/java/com/h8/ems/common/eta/` |
| State machine | `common/src/main/java/com/h8/ems/common/statemachine/` |
| Existing tests (reference style) | `common/src/test/java/com/h8/ems/common/` |
