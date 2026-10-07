package com.h8.ems.simulator.engine;

import com.h8.ems.simulator.config.ScenarioConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DemandGeneratorTest {

    @Test
    void sameSeedProducesSameIncidents() {
        ScenarioConfig config = new ScenarioConfig();
        Instant start = Instant.parse("2025-01-01T08:00:00Z");

        DemandGenerator gen1 = new DemandGenerator(config, 42L);
        List<IncidentDraw> draws1 = gen1.generate(start);

        DemandGenerator gen2 = new DemandGenerator(config, 42L);
        List<IncidentDraw> draws2 = gen2.generate(start);

        assertEquals(draws1.size(), draws2.size(), "Same seed must produce same count");
        for (int i = 0; i < draws1.size(); i++) {
            assertEquals(draws1.get(i).incidentId(), draws2.get(i).incidentId());
            assertEquals(draws1.get(i).arrivalTime(), draws2.get(i).arrivalTime());
            assertEquals(draws1.get(i).severity(), draws2.get(i).severity());
            assertEquals(draws1.get(i).sceneTimeSeconds(), draws2.get(i).sceneTimeSeconds(), 0.001);
        }
    }

    @Test
    void differentSeedsProduceDifferentIncidents() {
        ScenarioConfig config = new ScenarioConfig();
        Instant start = Instant.parse("2025-01-01T08:00:00Z");

        DemandGenerator gen1 = new DemandGenerator(config, 42L);
        List<IncidentDraw> draws1 = gen1.generate(start);

        DemandGenerator gen2 = new DemandGenerator(config, 99L);
        List<IncidentDraw> draws2 = gen2.generate(start);

        // Very unlikely to have same size AND same IDs
        boolean allSame = draws1.size() == draws2.size();
        if (allSame && !draws1.isEmpty()) {
            allSame = draws1.getFirst().incidentId().equals(draws2.getFirst().incidentId());
        }
        assertFalse(allSame, "Different seeds should produce different incidents");
    }

    @Test
    void generatesPlausibleNumberOfIncidents() {
        ScenarioConfig config = new ScenarioConfig();
        config.setDurationHours(24);
        config.setBaseDemandPerHour(5.0);
        Instant start = Instant.parse("2025-01-01T08:00:00Z");

        DemandGenerator gen = new DemandGenerator(config, 42L);
        List<IncidentDraw> draws = gen.generate(start);

        // ~5 incidents/hr * 24hr * avg factor ~0.86 = ~103 expected, ±50
        assertTrue(draws.size() > 30, "Expected at least 30 incidents, got " + draws.size());
        assertTrue(draws.size() < 300, "Expected fewer than 300 incidents, got " + draws.size());
    }

    @Test
    void allDrawsHavePreSampledRandomValues() {
        ScenarioConfig config = new ScenarioConfig();
        Instant start = Instant.parse("2025-01-01T08:00:00Z");

        DemandGenerator gen = new DemandGenerator(config, 42L);
        List<IncidentDraw> draws = gen.generate(start);

        for (IncidentDraw d : draws) {
            assertNotNull(d.incidentId());
            assertNotNull(d.arrivalTime());
            assertNotNull(d.location());
            assertNotNull(d.severity());
            assertNotNull(d.need());
            assertTrue(d.sceneTimeSeconds() > 0, "Scene time should be positive");
            assertTrue(d.travelNoiseFactor() > 0, "Noise factor should be positive");
            assertTrue(d.handoverSeconds() > 0, "Handover should be positive");
        }
    }
}
