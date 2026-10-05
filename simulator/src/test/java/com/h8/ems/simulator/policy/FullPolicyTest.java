package com.h8.ems.simulator.policy;

import com.h8.ems.common.eta.HaversineEta;
import com.h8.ems.common.model.*;
import com.h8.ems.common.scoring.*;
import com.h8.ems.simulator.engine.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FullPolicyTest {

    private static final Instant NOW = Instant.parse("2025-01-01T10:00:00Z");

    @Test
    void nameIsP3() {
        List<GeoPoint> zones = List.of(new GeoPoint(51.50, -0.12));
        CoverageModel coverage = new CoverageModel(8.0, zones);
        DispatchScorer scorer = new DispatchScorer(coverage);
        DestinationRanker ranker = new DestinationRanker();
        assertEquals("P3", new FullPolicy(scorer, ranker, 15).name());
    }

    @Test
    void usesRedeploymentReturnsTrue() {
        List<GeoPoint> zones = List.of(new GeoPoint(51.50, -0.12));
        CoverageModel coverage = new CoverageModel(8.0, zones);
        DispatchScorer scorer = new DispatchScorer(coverage);
        DestinationRanker ranker = new DestinationRanker();
        assertTrue(new FullPolicy(scorer, ranker, 15).usesRedeployment(),
                "P3 must enable redeployment");
    }

    @Test
    void selectsUnitViaScorer() {
        List<GeoPoint> zones = List.of(new GeoPoint(51.50, -0.12));
        CoverageModel coverage = new CoverageModel(8.0, zones);
        DispatchScorer scorer = new DispatchScorer(coverage);
        DestinationRanker ranker = new DestinationRanker();
        FullPolicy policy = new FullPolicy(scorer, ranker, 15);

        SimState state = new SimState(NOW);
        UUID u1 = UUID.randomUUID();
        state.addUnit(u1, "U1", UnitType.ALS, new GeoPoint(51.505, -0.125), UUID.randomUUID());

        IncidentDraw draw = new IncidentDraw(UUID.randomUUID(), NOW,
                new GeoPoint(51.50, -0.12), Severity.EMERGENCY, ClinicalNeed.GENERAL,
                false, 900, 1.0, 1200);

        TravelModel travelModel = new TravelModel(new HaversineEta());
        UUID selected = policy.selectUnit(draw, state, travelModel);
        assertNotNull(selected);
    }

    @Test
    void selectsHospitalViaRanker() {
        List<GeoPoint> zones = List.of(new GeoPoint(51.50, -0.12));
        CoverageModel coverage = new CoverageModel(8.0, zones);
        DispatchScorer scorer = new DispatchScorer(coverage);
        DestinationRanker ranker = new DestinationRanker();
        FullPolicy policy = new FullPolicy(scorer, ranker, 15);

        SimState state = new SimState(NOW);
        TravelModel travelModel = new TravelModel(new HaversineEta());
        HospitalModel hospitalModel = new HospitalModel();
        UUID hId = UUID.randomUUID();
        hospitalModel.initHospital(hId, "H1", new GeoPoint(51.52, -0.08),
                Set.of(ClinicalNeed.GENERAL, ClinicalNeed.TRAUMA), 20, 5, 3);
        hospitalModel.updateCapacityTimestamp(hId, NOW);

        IncidentDraw draw = new IncidentDraw(UUID.randomUUID(), NOW,
                new GeoPoint(51.50, -0.12), Severity.EMERGENCY, ClinicalNeed.GENERAL,
                false, 900, 1.0, 1200);

        UUID selected = policy.selectHospital(draw, state, travelModel, hospitalModel);
        assertNotNull(selected);
    }

    @Test
    void ablationFlagsDisableRedeployment() {
        List<GeoPoint> zones = List.of(new GeoPoint(51.50, -0.12));
        CoverageModel coverage = new CoverageModel(8.0, zones);
        DispatchScorer scorer = new DispatchScorer(coverage);
        DestinationRanker ranker = new DestinationRanker();

        FullPolicy.AblationFlags ablations = new FullPolicy.AblationFlags(true, false, false, false);
        FullPolicy policy = new FullPolicy(scorer, ranker, 15, ablations);

        assertFalse(policy.usesRedeployment(), "Ablation should disable redeployment");
        assertEquals("P3-no-redeploy", policy.name());
        assertEquals(ablations, policy.ablations());
    }

    @Test
    void ablationFlagsDisableHospitalRanking() {
        List<GeoPoint> zones = List.of(new GeoPoint(51.50, -0.12));
        CoverageModel coverage = new CoverageModel(8.0, zones);
        DispatchScorer scorer = new DispatchScorer(coverage);
        DestinationRanker ranker = new DestinationRanker();

        FullPolicy.AblationFlags ablations = new FullPolicy.AblationFlags(false, true, false, false);
        FullPolicy policy = new FullPolicy(scorer, ranker, 15, ablations);

        assertEquals("P3-no-hosp", policy.name());

        SimState state = new SimState(NOW);
        TravelModel travelModel = new TravelModel(new HaversineEta());
        HospitalModel hospitalModel = new HospitalModel();
        UUID hId = UUID.randomUUID();
        hospitalModel.initHospital(hId, "H1", new GeoPoint(51.52, -0.08),
                Set.of(ClinicalNeed.GENERAL, ClinicalNeed.TRAUMA), 20, 5, 3);
        hospitalModel.updateCapacityTimestamp(hId, NOW);

        IncidentDraw draw = new IncidentDraw(UUID.randomUUID(), NOW,
                new GeoPoint(51.50, -0.12), Severity.EMERGENCY, ClinicalNeed.GENERAL,
                false, 900, 1.0, 1200);

        UUID selected = policy.selectHospital(draw, state, travelModel, hospitalModel);
        assertEquals(hId, selected);
    }

    @Test
    void ablationFlagsNamedVariants() {
        List<GeoPoint> zones = List.of(new GeoPoint(51.50, -0.12));
        CoverageModel coverage = new CoverageModel(8.0, zones);
        DispatchScorer scorer = new DispatchScorer(coverage);
        DestinationRanker ranker = new DestinationRanker();

        FullPolicy covPolicy = new FullPolicy(scorer, ranker, 15,
                new FullPolicy.AblationFlags(false, false, true, false));
        assertEquals("P3-no-coverage", covPolicy.name());

        FullPolicy fatPolicy = new FullPolicy(scorer, ranker, 15,
                new FullPolicy.AblationFlags(false, false, false, true));
        assertEquals("P3-no-fatigue", fatPolicy.name());

        FullPolicy minPolicy = new FullPolicy(scorer, ranker, 15,
                new FullPolicy.AblationFlags(true, true, false, false));
        assertEquals("P3-minimal", minPolicy.name());
    }
}
