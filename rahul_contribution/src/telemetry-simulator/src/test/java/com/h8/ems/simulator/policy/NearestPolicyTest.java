package com.h8.ems.simulator.policy;

import com.h8.ems.common.eta.HaversineEta;
import com.h8.ems.common.model.*;
import com.h8.ems.simulator.engine.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NearestPolicyTest {

    private static final Instant NOW = Instant.parse("2025-01-01T10:00:00Z");

    @Test
    void nameIsB1() {
        assertEquals("B1", new NearestPolicy().name());
    }

    @Test
    void selectsClosestUnit() {
        SimState state = new SimState(NOW);
        GeoPoint incidentLoc = new GeoPoint(51.50, -0.12);

        // Farther unit
        UUID farId = UUID.randomUUID();
        state.addUnit(farId, "U1", UnitType.ALS, new GeoPoint(51.55, -0.20), UUID.randomUUID());

        // Closer unit
        UUID nearId = UUID.randomUUID();
        state.addUnit(nearId, "U2", UnitType.BLS, new GeoPoint(51.505, -0.125), UUID.randomUUID());

        IncidentDraw draw = new IncidentDraw(UUID.randomUUID(), NOW, incidentLoc,
                Severity.EMERGENCY, ClinicalNeed.GENERAL, false, 900, 1.0, 1200);

        TravelModel travelModel = new TravelModel(new HaversineEta());
        UUID selected = new NearestPolicy().selectUnit(draw, state, travelModel);

        assertEquals(nearId, selected, "Should pick the closer unit");
    }

    @Test
    void returnsNullWhenNoUnitsAvailable() {
        SimState state = new SimState(NOW);
        IncidentDraw draw = new IncidentDraw(UUID.randomUUID(), NOW,
                new GeoPoint(51.50, -0.12), Severity.EMERGENCY, ClinicalNeed.GENERAL,
                false, 900, 1.0, 1200);

        TravelModel travelModel = new TravelModel(new HaversineEta());
        assertNull(new NearestPolicy().selectUnit(draw, state, travelModel));
    }

    @Test
    void doesNotUseRedeployment() {
        assertFalse(new NearestPolicy().usesRedeployment());
    }
}
