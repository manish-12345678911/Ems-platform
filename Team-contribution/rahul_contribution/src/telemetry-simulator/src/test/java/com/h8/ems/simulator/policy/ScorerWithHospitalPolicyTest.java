package com.h8.ems.simulator.policy;

import com.h8.ems.common.eta.HaversineEta;
import com.h8.ems.common.model.*;
import com.h8.ems.common.scoring.*;
import com.h8.ems.simulator.engine.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ScorerWithHospitalPolicyTest {

    private static final Instant NOW = Instant.parse("2025-01-01T10:00:00Z");
    private ScorerWithHospitalPolicy policy;
    private SimState state;
    private TravelModel travelModel;
    private HospitalModel hospitalModel;

    @BeforeEach
    void setUp() {
        List<GeoPoint> zones = List.of(new GeoPoint(51.50, -0.12));
        CoverageModel coverage = new CoverageModel(8.0, zones);
        DispatchScorer scorer = new DispatchScorer(coverage);
        DestinationRanker ranker = new DestinationRanker();
        policy = new ScorerWithHospitalPolicy(scorer, ranker, 15);

        state = new SimState(NOW);
        UUID u1 = UUID.randomUUID();
        state.addUnit(u1, "U1", UnitType.ALS, new GeoPoint(51.505, -0.125), UUID.randomUUID());

        travelModel = new TravelModel(new HaversineEta());
        hospitalModel = new HospitalModel();

        UUID h1 = UUID.nameUUIDFromBytes("H1".getBytes());
        hospitalModel.initHospital(h1, "Hospital 1", new GeoPoint(51.52, -0.08),
                Set.of(ClinicalNeed.GENERAL, ClinicalNeed.TRAUMA, ClinicalNeed.CARDIAC),
                20, 5, 3);
        hospitalModel.updateCapacityTimestamp(h1, NOW);

        UUID h2 = UUID.nameUUIDFromBytes("H2".getBytes());
        hospitalModel.initHospital(h2, "Hospital 2", new GeoPoint(51.48, -0.15),
                Set.of(ClinicalNeed.GENERAL, ClinicalNeed.STROKE),
                15, 3, 2);
        hospitalModel.updateCapacityTimestamp(h2, NOW);
    }

    @Test
    void nameIsP2() {
        assertEquals("P2", policy.name());
    }

    @Test
    void delegatesUnitSelectionToScorer() {
        IncidentDraw draw = new IncidentDraw(UUID.randomUUID(), NOW,
                new GeoPoint(51.50, -0.12), Severity.EMERGENCY, ClinicalNeed.GENERAL,
                false, 900, 1.0, 1200);

        UUID selected = policy.selectUnit(draw, state, travelModel);
        assertNotNull(selected, "Should delegate unit selection to P1 (ScorerPolicy)");
    }

    @Test
    void usesRankerForHospitalSelection() {
        IncidentDraw draw = new IncidentDraw(UUID.randomUUID(), NOW,
                new GeoPoint(51.50, -0.12), Severity.EMERGENCY, ClinicalNeed.GENERAL,
                false, 900, 1.0, 1200);

        UUID hospitalId = policy.selectHospital(draw, state, travelModel, hospitalModel);
        assertNotNull(hospitalId, "Should select a hospital via DestinationRanker");
    }

    @Test
    void doesNotUseRedeployment() {
        assertFalse(policy.usesRedeployment());
    }
}
