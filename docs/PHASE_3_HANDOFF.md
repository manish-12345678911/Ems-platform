# Phase 3 Completion & Phase 4 Implementation Handoff — EMS Platform

> **Status**: **Phases 0, 1, 2, and Phase 3 Core Implementation are 100% COMPLETE & PASSING** ✅
> - `common`: 50/50 tests passing (plain Java 21, ArchUnit clean, jqwik property tests).
> - `contracts`: Event records (`IncidentEvent`, `UnitStatusEvent`, `LocationUpdate`, `DispatchDecision`, `PreArrivalAlert`, `AuditEvent`), DTOs (`CreateIncidentRequest`, `EtaRequest`, `EtaResponse`), and Outbox records (`OutboxRecord`) (1/1 test passing).
> - `simulator`: 72/72 tests passing, determinism verified (byte-identical CSV outputs), fat JAR packaged and runnable, ablations implemented (`P3-no-redeploy`, `P3-no-hosp`, `P3-no-coverage`, `P3-no-fatigue`).
> - `routing-service`: 5/5 tests passing (`RoutingEtaServiceTest`, `RoutingControllerTest` with Resilience4j circuit breaker and HaversineEta fallback).
> - `incident-service`: 12/12 tests passing (`IncidentControllerTest`, `OutboxRelayTest`, `IncidentServiceTest`, `CallerHashUtilTest` with single-tx incident + outbox creation, salted caller phone hashing, scheduled outbox relay).
> - **Total Repository Test Suite**: **140 tests passing across all 13 modules** (`mvn test` BUILD SUCCESS).
>
> **This file gives any model or developer the complete blueprint to implement Phase 4 (Tracking & Dispatch Services) immediately.**

---

## 1. Environment & Build Commands

| Item | Value |
|------|-------|
| OS | Windows 11, PowerShell |
| Java | JDK 27 at `C:\Program Files\Java\jdk-27` |
| Maven | 3.9.9 at `C:\tools\apache-maven-3.9.9` |
| Workspace | `c:\ambulance` |
| Environment setup | `$env:JAVA_HOME = "C:\Program Files\Java\jdk-27"; $env:PATH = "C:\tools\apache-maven-3.9.9\bin;$env:PATH"` |
| Compile all modules | `mvn compile` |
| Run all unit tests | `mvn test` |
| Test specific module | `mvn test -pl <module-name> -am` |
| Package platform | `mvn package -DskipTests` |
| Start infra services | `docker compose up -d postgres redis kafka` |

---

## 2. Hard Architectural Rules (from `AGENTS.md` — MUST FOLLOW)

1. **`common` and `simulator` must NOT depend on Spring, JPA, Kafka or Redis.** Plain Java 21 only.
2. **`DispatchScorer`, `CoverageModel`, `DestinationRanker` live only in `common`.** Services import them; never copy or duplicate scoring logic.
3. **A unit is reserved only by a conditional UPDATE (`status = AVAILABLE`).** Never read-then-write.
4. **Every Kafka consumer is idempotent** (dedupe on `eventId` via `processed_event` table). **Every state change that must be published uses the outbox table.**
5. **All unit positions and hospital capacities carry a timestamp and TTL.** Stale data is penalised or ignored, never trusted.
6. **No real patient or caller data.** Caller phone is stored only as a salted SHA-256 hash.
7. **Simulator runs are deterministic for a given seed.** Use common random numbers across policies.
8. **Every new class gets a unit test.** Concurrency, state machine, and scorer get property tests (`jqwik`).
9. **Do not invent library APIs.** Check pinned dependency versions in `pom.xml`.
10. **Never commit secrets.** Use `.env.example` and Docker secrets.

---

## 3. What Has Been Completed in Phase 3

### 3.1 `routing-service` (Port 8084) — Complete & Verified ✅
- **Classes**:
 - `RoutingEtaService`: Injects primary `EtaProvider` (GraphHopper placeholder for Phase 5) with fallback to `HaversineEta` from `common`.
 - `@CircuitBreaker(name = "routingEta", fallbackMethod = "calculateFallback")`: When primary fails, circuit breaker triggers fallback to `HaversineEta` returning `fallback = true`.
 - `RoutingController`: Exposes `POST /eta` (accepts `EtaRequest`) and `GET /eta` (accepts `fromLat, fromLon, toLat, toLon`).
 - `SecurityConfig`: Configures public access for `/eta/**` and `/actuator/**`.
- **Tests (5 tests)**:
 - `RoutingEtaServiceTest`: Verifies fallback without primary, primary routing, and circuit breaker fallback method.
 - `RoutingControllerTest`: WebMvc tests for POST and GET endpoints.

### 3.2 `incident-service` (Port 8081) — Complete & Verified ✅
- **Database & Entities**:
 - `V1__init_incident_schema.sql`: Schema `incident`, tables `incident`, `outbox_event` (with `idx_outbox_unpub` partial index), `processed_event`.
 - `IncidentEntity`: Mapped to `incident.incident`. PostGIS `location GEOGRAPHY(Point, 4326)` using JTS `Point`. Clean JSON serialization with `getLat()`, `getLon()` and `@JsonIgnore` on raw geometry.
 - `OutboxEventEntity`: Mapped to `incident.outbox_event`. Stores `aggregateId`, `topic`, `eventKey`, `payload` (JSONB), `createdAt`, `publishedAt`.
- **Business Logic & Services**:
 - `CallerHashUtil`: Computes salted SHA-256 hash of caller phone (Rule #6).
 - `IncidentService`: Atomically creates `IncidentEntity` and `OutboxEventEntity` within a single `@Transactional` method (Rule #4).
 - `OutboxRelay`: Scheduled component (`@Scheduled(fixedDelay = 500)`) reading unpublished outbox records, publishing to Kafka topic `incident.events`, and updating `publishedAt` upon broker ACK. If Kafka is unavailable, records remain pending for subsequent retries without event loss.
 - `IncidentController`: Exposes `POST /incidents` (returns 201 Created with `incidentId`) and `GET /incidents/{id}`.
 - `SecurityConfig`: Configures access for `/incidents/**` and `/actuator/**`.
- **Tests (12 tests)**:
 - `IncidentServiceTest`: Verifies atomic creation of incident + outbox event and phone hashing.
 - `OutboxRelayTest`: Verifies publishing, asynchronous ACK updating, and fault-tolerance under broker connection loss.
 - `CallerHashUtilTest`: Verifies SHA-256 64-char hex format and salt variation.
 - `IncidentControllerTest`: WebMvc tests for `POST /incidents` and `GET /incidents/{id}`.

### 3.3 `contracts` — Updated & Verified ✅
- Added `OutboxRecord` record representing outbox message transfer.
- Added `OutboxRecordTest`.

---

## 4. Current Test Baseline

```
 Common Library .................................. 50 tests ✅
 Contracts ....................................... 1 test ✅
 Simulator ....................................... 72 tests ✅
 Incident Service ................................ 12 tests ✅
 Routing Service ................................. 5 tests ✅
-----------------------------------------------------------------
Total Automated Tests: 140 tests ✅ (0 failures, 0 errors)
```

---

## 5. Detailed Implementation Blueprint for Phase 4

Phase 4 consists of two interconnected services:
1. **`tracking-service`**: Real-time ambulance tracking in Redis GEO with atomic timestamp comparison and heartbeat pruner.
2. **`dispatch-service`**: Candidate ranking using `DispatchScorer` from `common`, atomic unit reservation via conditional SQL `UPDATE`, and transactional outbox publishing.

```
 ┌─────────────────────────┐
 │ Kafka: unit.location │
 └───────────┬─────────────┘
 │
 ▼
 ┌─────────────────────────┐
 │ tracking-service │
 │ (Port 8083, Redis) │
 └───────────┬─────────────┘
 │
 Redis GEO: Nearby Unit IDs
 │
 ▼
┌──────────────────┐ Candidates ┌─────────────────────────┐
│ routing-service │───────────────▶│ dispatch-service │
│ (Port 8084) │ ETAs │ (Port 8082) │
└──────────────────┘ └───────────┬─────────────┘
 │
 1. Conditional UPDATE
 2. Outbox: dispatch.decisions
 3. Outbox: unit.status
 ▼
 ┌─────────────────────────┐
 │ PostgreSQL & Kafka │
 └─────────────────────────┘
```

---

### 5.1 Service 1: `tracking-service` (Port 8083)

#### A. Responsibilities
1. Consume `LocationUpdate` events from Kafka topic `unit.location`.
2. Update Redis atomically using a Lua script to reject out-of-order/stale GPS pings.
3. Expose nearby available units via Redis GEO queries for `dispatch-service`.
4. Run a background pruner job to detect expired unit heartbeats, remove them from GEO, and publish `UnitStatusEvent` (`to = OFFLINE`).

#### B. Architecture Correction #7: Atomic Redis Lua Script
Reading timestamp, then executing `GEOADD` and `SET` in separate calls is **not atomic** and race-prone under concurrency.
Create `TrackingRedisService` executing this Redis script:

```lua
-- KEYS[1] = unit:ts:{unitId}
-- KEYS[2] = units:geo
-- ARGV[1] = epochMs (number)
-- ARGV[2] = lon (double)
-- ARGV[3] = lat (double)
-- ARGV[4] = unitId (string)

local current_ts = redis.call('GET', KEYS[1])
if not current_ts or tonumber(ARGV[1]) > tonumber(current_ts) then
 redis.call('SET', KEYS[1], ARGV[1])
 redis.call('GEOADD', KEYS[2], ARGV[2], ARGV[3], ARGV[4])
 return 1
else
 return 0 -- out of order update rejected
end
```

#### C. Redis Keys & Data Structures
- `units:geo`: Redis Geospatial sorted set holding members `{unitId}` with lat/lon coordinates.
- `unit:ts:{unitId}`: String key storing last received `epochMs`.
- `unit:info:{unitId}`: Hash storing speed, last update instant.

#### D. REST Endpoints for `dispatch-service`
- `GET /tracking/nearby?lat={lat}&lon={lon}&radiusKm={radiusKm}&limit={limit}`:
 - Executes `GEOSEARCH units:geo FROMLONLAT {lon} {lat} BYRADIUS {radiusKm} KM ASC WITHDIST COUNT {limit}`.
 - Returns `List<NearbyUnitResponse>` containing `unitId`, `distanceKm`, and `lastPositionAt`.

#### E. Heartbeat TTL & Pruner
- Heartbeat TTL: default 90 seconds (from `ScorerParams.staleTtlSeconds`).
- `@Scheduled(fixedDelay = 15000)` `TrackingPruner`:
 - Scans `units:geo` members.
 - For any unit where `nowEpochMs - unit:ts:{unitId} > ttlMs`:
 - Remove from `units:geo` (`ZREM`).
 - Publish `UnitStatusEvent(UUID.randomUUID(), unitId, "AVAILABLE", "OFFLINE", Instant.now())` to `unit.status`.

#### F. Unit & Integration Tests to Write
1. `TrackingRedisServiceTest`: Verifies Lua script rejects timestamps where `newTs <= currentTs` and accepts `newTs > currentTs`.
2. `LocationConsumerTest`: Verifies Kafka listener parses `LocationUpdate` and delegates to tracking service.
3. `TrackingControllerTest`: MockMvc tests for `GET /tracking/nearby`.
4. `TrackingPrunerTest`: Verifies expired units are detected and status event emitted.

---

### 5.2 Service 2: `dispatch-service` (Port 8082)

#### A. Responsibilities
1. Maintain unit roster and station references in `dispatch` schema (`V1__init_dispatch_schema.sql` already provided).
2. Rank nearby candidates for an incident using `DispatchScorer` and `CoverageModel` from `common`.
3. Reserve the winning candidate unit using **atomic conditional SQL UPDATE** (Hard Rule #3).
4. Persist assignment with `ranked_snapshot` JSON and write outbox events for `dispatch.decisions` and `unit.status`.
5. Support dispatcher override with reason and crew reject / re-dispatch workflow.

#### B. Architecture Correction #4: Conditional UPDATE Reservation
**Hard Rule #3**: Never read-then-write! Never bump version manually while using `@Version`.
In `AmbulanceUnitRepository`:
```java
@Modifying
@Query("UPDATE AmbulanceUnitEntity u SET u.status = 'DISPATCHED' " +
 "WHERE u.id = :unitId AND u.status = 'AVAILABLE'")
int reserveIfAvailable(@Param("unitId") UUID unitId);
```
- If return value == `1`: reservation succeeded.
- If return value == `0`: reservation failed (concurrency race or unit became unavailable). Return 409 Conflict or re-trigger ranking.

#### C. Architecture Correction #5: Configurable Radius with Rural Fallback
In `DispatchCandidateService`:
- Config parameters: `dispatch.nearby-radius-km: 25.0`, `dispatch.max-candidates: 15`, `dispatch.max-radius-km: 100.0`.
- If candidate query with initial radius returns 0 available units, automatically expand search radius (e.g. $2 \times$ up to `max-radius-km`) to handle rural scenarios like S4.

#### D. Candidate Ranking Workflow
1. Given `IncidentSnapshot` (location, severity, need, requiresAls, receivedAt):
2. Query `tracking-service` for nearby available units: `GET /tracking/nearby?lat=...&lon=...&radiusKm=25`.
3. Fetch candidate `AmbulanceUnitEntity` rows from database where `id IN (:ids) AND status = 'AVAILABLE'`.
4. For each candidate unit:
 - Call `routing-service` `POST /eta` with `(unitLat, unitLon, incidentLat, incidentLon)`.
 - Build `UnitSnapshot` from entity and live position.
5. Invoke `DispatchScorer.score(unitSnapshot, incidentSnapshot, etaSeconds, Instant.now(), availableSnapshots)` and `DispatchScorer.breakdown(...)` from `common` (**Rule #2: imported directly, never copied**).
6. Sort candidates by score ascending (lowest score is optimal).
7. Return candidate list with breakdown components:
 - ETA penalty ($0.40 \times \text{ETA} \times \text{Severity.scaleFactor}$).
 - Capability penalty ($0.25$, BLS vs ALS-needed).
 - Fatigue penalty ($0.10$, shift $> 10$ h).
 - Coverage loss ($0.15 \times \text{lossIfRemoved}$).
 - Staleness penalty ($0.10$, position $> 90$ s old).

#### E. Dispatch Confirmation Workflow (`POST /dispatch`)
```java
@Transactional
public DispatchResult dispatchUnit(DispatchUnitRequest req) {
 // 1. Conditional update reservation
 int updated = unitRepository.reserveIfAvailable(req.unitId());
 if (updated == 0) {
 throw new UnitNotAvailableException("Unit " + req.unitId() + " is no longer available");
 }

 // 2. Insert assignment with ranked snapshot
 AssignmentEntity assignment = new AssignmentEntity();
 assignment.setIncidentId(req.incidentId());
 assignment.setUnitId(req.unitId());
 assignment.setRankedSnapshot(req.rankedSnapshotJson());
 assignment.setChosenBy(req.chosenBy()); // AUTO or DISPATCHER
 assignment.setDecidedAt(Instant.now());
 assignmentRepository.save(assignment);

 // 3. Insert OutboxEvent for dispatch.decisions
 DispatchDecision decision = new DispatchDecision(
 UUID.randomUUID(), req.incidentId(), req.unitId(),
 req.chosenBy(), req.rankedSnapshotJson(), 1
 );
 outboxRepository.save(new OutboxEventEntity(
 UUID.randomUUID(), req.incidentId(), "dispatch.decisions",
 req.incidentId().toString(), toJson(decision), Instant.now(), null
 ));

 // 4. Insert OutboxEvent for unit.status
 UnitStatusEvent statusEvent = new UnitStatusEvent(
 UUID.randomUUID(), req.unitId(), "AVAILABLE", "DISPATCHED", Instant.now(), 1
 );
 outboxRepository.save(new OutboxEventEntity(
 UUID.randomUUID(), req.unitId(), "unit.status",
 req.unitId().toString(), toJson(statusEvent), Instant.now(), null
 ));

 return new DispatchResult(assignment.getId(), req.unitId(), "DISPATCHED");
}
```

#### F. Unit & Concurrency Tests to Write
1. **Concurrency Test (Rule #8)**:
 - Spawn 50 concurrent threads attempting to call `reserveIfAvailable(unitId)` on a single `AVAILABLE` unit.
 - Assert: Exactly 1 thread succeeds (returns 1), 49 threads return 0.
2. `DispatchScorerIntegrationTest`:
 - Verifies scoring order matches `DispatchScorer` from `common`.
3. `RuralFallbackTest`:
 - Verifies radius expansion when 0 units found within 25 km.
4. `DispatchControllerTest`:
 - WebMvc tests for `GET /dispatch/candidates` and `POST /dispatch`.

---

## 6. Phase 4 Step-by-Step Execution Plan for the Next Agent

### Step 1: `tracking-service`
1. Create `TrackingRedisService.java` loading and executing the atomic Lua script.
2. Create `LocationConsumer.java` listening on `unit.location`.
3. Create `TrackingController.java` exposing `GET /tracking/nearby`.
4. Create `TrackingPruner.java` with `@Scheduled` heartbeat check.
5. Create unit tests for Lua script, consumer, controller, and pruner.
6. Verify: `mvn test -pl tracking-service -am`.

### Step 2: `dispatch-service`
1. Add `hibernate-spatial` to `dispatch-service/pom.xml` if needed for `station` and `ambulance_unit` locations.
2. Create JPA entities: `AmbulanceUnitEntity`, `StationEntity`, `AssignmentEntity`, `OutboxEventEntity`, `ProcessedEventEntity`.
3. Create `AmbulanceUnitRepository` with `@Modifying` conditional `reserveIfAvailable`.
4. Create `DispatchRankingService` importing `DispatchScorer` from `common`.
5. Create `DispatchExecutionService` with `@Transactional` dispatch + outbox creation.
6. Create `DispatchController` (`/dispatch/candidates`, `/dispatch`).
7. Write jqwik concurrency property test (50 threads $\to$ 1 winner).
8. Write unit & WebMvc tests.
9. Verify: `mvn test -pl dispatch-service -am`.

### Step 3: Full Workspace Verification
Run `mvn test` across all 13 modules to ensure zero regression across the platform.

---

## 7. Architecture Corrections Checklist for Phase 4

| # | Item | Status | Action in Phase 4 |
|---|------|:------:|-------------------|
| 4 | Drop manual version bumping in conditional update | 🔲 | Use pure SQL conditional UPDATE `WHERE id = ? AND status = 'AVAILABLE'` without manual version increment. |
| 5 | Make radius/limit configurable with rural fallback | 🔲 | Implement adaptive radius expansion up to `max-radius-km` when initial search yields 0 candidates. |
| 7 | Use Lua script for atomic Redis timestamp compare | 🔲 | Implement Lua script in `TrackingRedisService` for atomic `unit:ts` check and `GEOADD`. |
