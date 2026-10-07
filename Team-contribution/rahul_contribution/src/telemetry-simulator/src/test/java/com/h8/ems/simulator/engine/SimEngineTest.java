package com.h8.ems.simulator.engine;

import com.h8.ems.common.eta.HaversineEta;
import com.h8.ems.common.model.*;
import com.h8.ems.common.scoring.*;
import com.h8.ems.simulator.config.ScenarioConfig;
import com.h8.ems.simulator.policy.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SimEngineTest {

    @Test
    void deterministicRunProducesSameResults() {
        ScenarioConfig config = new ScenarioConfig();
        config.setName("test");
        config.setDurationHours(4);
        config.setBaseDemandPerHour(3.0);

        Instant simStart = Instant.parse("2025-01-01T08:00:00Z");

        List<SimEngine.IncidentResult> run1 = runSimulation(config, 42L, new NearestPolicy());
        List<SimEngine.IncidentResult> run2 = runSimulation(config, 42L, new NearestPolicy());

        assertEquals(run1.size(), run2.size(), "Same seed must produce same result count");
        for (int i = 0; i < run1.size(); i++) {
            assertEquals(run1.get(i).incidentId(), run2.get(i).incidentId());
            assertEquals(run1.get(i).responseTimeSec(), run2.get(i).responseTimeSec(), 0.001);
        }
    }

    @Test
    void b1PolicyProducesPlausibleResponseTimes() {
        ScenarioConfig config = new ScenarioConfig();
        config.setDurationHours(8);
        config.setBaseDemandPerHour(3.0);

        List<SimEngine.IncidentResult> results = runSimulation(config, 42L, new NearestPolicy());

        List<Double> responseTimes = results.stream()
                .filter(SimEngine.IncidentResult::served)
                .map(SimEngine.IncidentResult::responseTimeSec)
                .filter(r -> r >= 0)
                .toList();

        assertFalse(responseTimes.isEmpty(), "Should have served incidents");
        double mean = responseTimes.stream().mapToDouble(d -> d).average().orElse(0);
        // Urban with 20 units, mean response should be ~200-1200 sec (3-20 min)
        assertTrue(mean > 60, "Mean response too low: " + mean);
        assertTrue(mean < 3600, "Mean response too high: " + mean);
    }

    @Test
    void allPoliciesCanRun() {
        ScenarioConfig config = new ScenarioConfig();
        config.setDurationHours(8);
        config.setBaseDemandPerHour(5.0);

        List<GeoPoint> zones = List.of(new GeoPoint(51.50, -0.12));
        CoverageModel coverage = new CoverageModel(8.0, zones);
        DispatchScorer scorer = new DispatchScorer(coverage);
        DestinationRanker ranker = new DestinationRanker();

        List<Policy> policies = List.of(
                new NearestPolicy(),
                new NearestAlsAwarePolicy(),
                new ScorerPolicy(scorer),
                new ScorerWithHospitalPolicy(scorer, ranker, 15),
                new FullPolicy(scorer, ranker, 15)
        );

        for (Policy p : policies) {
            List<SimEngine.IncidentResult> results = runSimulation(config, 42L, p);
            assertNotNull(results, "Policy " + p.name() + " should return results");
            assertFalse(results.isEmpty(), "Policy " + p.name() + " should have results");
        }
    }

    private List<SimEngine.IncidentResult> runSimulation(ScenarioConfig config, long seed, Policy policy) {
        Instant simStart = Instant.parse("2025-01-01T08:00:00Z");
        DemandGenerator gen = new DemandGenerator(config, seed);
        List<IncidentDraw> draws = gen.generate(simStart);

        SimState state = new SimState(simStart);
        Random rng = new Random(42);
        for (int i = 0; i < config.getFleetSize(); i++) {
            UUID unitId = UUID.nameUUIDFromBytes(("unit-" + i).getBytes());
            UUID stationId = UUID.nameUUIDFromBytes(("station-" + (i % 5)).getBytes());
            UnitType type = (i < config.getFleetSize() * config.getAlsFraction())
                    ? UnitType.ALS : UnitType.BLS;
            double lat = config.getMinLat() + rng.nextDouble() * (config.getMaxLat() - config.getMinLat());
            double lon = config.getMinLon() + rng.nextDouble() * (config.getMaxLon() - config.getMinLon());
            state.addUnit(unitId, "U" + (i + 1), type, new GeoPoint(lat, lon), stationId);
        }

        TravelModel travelModel = new TravelModel(new HaversineEta());
        HospitalModel hospitalModel = new HospitalModel();
        for (int i = 0; i < 5; i++) {
            UUID id = UUID.nameUUIDFromBytes(("hospital-" + i).getBytes());
            double lat = config.getMinLat() + (i + 0.5) / 5.0 * (config.getMaxLat() - config.getMinLat());
            hospitalModel.initHospital(id, "H" + (i + 1), new GeoPoint(lat, -0.12),
                    Set.of(ClinicalNeed.GENERAL, ClinicalNeed.TRAUMA, ClinicalNeed.CARDIAC),
                    20, 5, 3);
        }

        List<GeoPoint> zoneCentroids = List.of(new GeoPoint(51.50, -0.12));
        CoverageModel coverageModel = new CoverageModel(8.0, zoneCentroids);
        RedeploymentPlanner redeployPlanner = new RedeploymentPlanner(coverageModel);

        SimEngine engine = new SimEngine(state, travelModel, hospitalModel,
                policy, config, coverageModel, redeployPlanner);
        engine.scheduleIncidents(draws);
        return engine.run();
    }
}
