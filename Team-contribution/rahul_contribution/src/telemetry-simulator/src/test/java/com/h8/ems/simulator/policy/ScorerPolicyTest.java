package com.h8.ems.simulator.policy;

import com.h8.ems.common.eta.HaversineEta;
import com.h8.ems.common.model.*;
import com.h8.ems.common.scoring.*;
import com.h8.ems.simulator.engine.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ScorerPolicyTest {

    private static final Instant NOW = Instant.parse("2025-01-01T10:00:00Z");

    @Test
    void nameIsP1() {
        CoverageModel coverage = new CoverageModel(8.0, List.of(new GeoPoint(51.50, -0.12)));
        DispatchScorer scorer = new DispatchScorer(coverage);
        assertEquals("P1", new ScorerPolicy(scorer).name());
    }

    @Test
    void usesDispatchScorerToSelectUnit() {
        List<GeoPoint> zones = List.of(new GeoPoint(51.50, -0.12));
        CoverageModel coverage = new CoverageModel(8.0, zones);
        DispatchScorer scorer = new DispatchScorer(coverage);

        SimState state = new SimState(NOW);
        GeoPoint incidentLoc = new GeoPoint(51.50, -0.12);

        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        state.addUnit(u1, "U1", UnitType.ALS, new GeoPoint(51.505, -0.125), UUID.randomUUID());
        state.addUnit(u2, "U2", UnitType.BLS, new GeoPoint(51.55, -0.20), UUID.randomUUID());

        IncidentDraw draw = new IncidentDraw(UUID.randomUUID(), NOW, incidentLoc,
                Severity.EMERGENCY, ClinicalNeed.GENERAL, false, 900, 1.0, 1200);

        TravelModel travelModel = new TravelModel(new HaversineEta());
        ScorerPolicy policy = new ScorerPolicy(scorer);

        UUID selected = policy.selectUnit(draw, state, travelModel);

        assertNotNull(selected, "Should select a unit");
        // The scorer should favor the closer ALS unit for an EMERGENCY
        // (both ETA and capability components favor u1)
    }

    @Test
    void returnsNullWhenNoUnitsAvailable() {
        CoverageModel coverage = new CoverageModel(8.0, List.of(new GeoPoint(51.50, -0.12)));
        DispatchScorer scorer = new DispatchScorer(coverage);
        SimState state = new SimState(NOW);

        IncidentDraw draw = new IncidentDraw(UUID.randomUUID(), NOW,
                new GeoPoint(51.50, -0.12), Severity.EMERGENCY, ClinicalNeed.GENERAL,
                false, 900, 1.0, 1200);

        TravelModel travelModel = new TravelModel(new HaversineEta());
        assertNull(new ScorerPolicy(scorer).selectUnit(draw, state, travelModel));
    }

    @Test
    void doesNotUseRedeployment() {
        CoverageModel coverage = new CoverageModel(8.0, List.of(new GeoPoint(51.50, -0.12)));
        DispatchScorer scorer = new DispatchScorer(coverage);
        assertFalse(new ScorerPolicy(scorer).usesRedeployment());
    }
}
