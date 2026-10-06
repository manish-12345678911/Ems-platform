package com.h8.ems.simulator.policy;

import com.h8.ems.common.eta.HaversineEta;
import com.h8.ems.common.model.*;
import com.h8.ems.simulator.engine.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NearestAlsAwarePolicyTest {

    private static final Instant NOW = Instant.parse("2025-01-01T10:00:00Z");

    @Test
    void nameIsB2() {
        assertEquals("B2", new NearestAlsAwarePolicy().name());
    }

    @Test
    void prefersAlsWhenRequired() {
        SimState state = new SimState(NOW);
        GeoPoint incidentLoc = new GeoPoint(51.50, -0.12);

        // Closer BLS
        UUID blsId = UUID.randomUUID();
        state.addUnit(blsId, "U1", UnitType.BLS, new GeoPoint(51.501, -0.121), UUID.randomUUID());

        // Farther ALS
        UUID alsId = UUID.randomUUID();
        state.addUnit(alsId, "U2", UnitType.ALS, new GeoPoint(51.52, -0.10), UUID.randomUUID());

        IncidentDraw draw = new IncidentDraw(UUID.randomUUID(), NOW, incidentLoc,
                Severity.CRITICAL, ClinicalNeed.TRAUMA, true, 900, 1.0, 1200);

        TravelModel travelModel = new TravelModel(new HaversineEta());
        UUID selected = new NearestAlsAwarePolicy().selectUnit(draw, state, travelModel);

        assertEquals(alsId, selected, "Should prefer ALS when requiresAls=true");
    }

    @Test
    void fallsBackToNearestWhenAlsNotRequired() {
        SimState state = new SimState(NOW);
        GeoPoint incidentLoc = new GeoPoint(51.50, -0.12);

        // Closer BLS
        UUID blsId = UUID.randomUUID();
        state.addUnit(blsId, "U1", UnitType.BLS, new GeoPoint(51.501, -0.121), UUID.randomUUID());

        // Farther ALS
        UUID alsId = UUID.randomUUID();
        state.addUnit(alsId, "U2", UnitType.ALS, new GeoPoint(51.55, -0.20), UUID.randomUUID());

        IncidentDraw draw = new IncidentDraw(UUID.randomUUID(), NOW, incidentLoc,
                Severity.URGENT, ClinicalNeed.GENERAL, false, 900, 1.0, 1200);

        TravelModel travelModel = new TravelModel(new HaversineEta());
        UUID selected = new NearestAlsAwarePolicy().selectUnit(draw, state, travelModel);

        assertEquals(blsId, selected, "Should pick nearest when ALS not required");
    }

    @Test
    void fallsBackToBlsWhenNoAlsAvailable() {
        SimState state = new SimState(NOW);
        GeoPoint incidentLoc = new GeoPoint(51.50, -0.12);

        UUID blsId = UUID.randomUUID();
        state.addUnit(blsId, "U1", UnitType.BLS, new GeoPoint(51.501, -0.121), UUID.randomUUID());

        IncidentDraw draw = new IncidentDraw(UUID.randomUUID(), NOW, incidentLoc,
                Severity.CRITICAL, ClinicalNeed.TRAUMA, true, 900, 1.0, 1200);

        TravelModel travelModel = new TravelModel(new HaversineEta());
        UUID selected = new NearestAlsAwarePolicy().selectUnit(draw, state, travelModel);

        assertEquals(blsId, selected, "Should fall back to BLS when no ALS available");
    }

    @Test
    void doesNotUseRedeployment() {
        assertFalse(new NearestAlsAwarePolicy().usesRedeployment());
    }
}
