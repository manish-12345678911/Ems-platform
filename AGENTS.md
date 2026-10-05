Project: H8 capability-aware ambulance dispatch (Java 21, Spring Boot 3, Maven multi-module).
Hard rules:
1. `common` and `simulator` must NOT depend on Spring, JPA, Kafka or Redis. Plain Java 21 only.
2. DispatchScorer, CoverageModel, DestinationRanker live only in `common`. Services and simulator import them. Never copy them.
3. A unit is reserved only by a conditional UPDATE (status = AVAILABLE). Never read-then-write.
4. Every Kafka consumer is idempotent (dedupe on eventId). Every state change that must be published uses the outbox table.
5. All unit positions and hospital capacities carry a timestamp and TTL. Stale data is penalised or ignored, never trusted.
6. No real patient or caller data. Caller phone is stored only as a salted hash.
7. Simulator runs are deterministic for a given seed. Use common random numbers across policies.
8. Every new class gets a unit test. Concurrency, state machine and scorer get property tests (jqwik).
9. Do not invent library APIs. Check GraphHopper, Spring Data Redis, Keycloak docs for the pinned version.
10. Never commit secrets. Use .env.example and Docker secrets.
