package com.h8.ems.common.scoring;

import com.h8.ems.common.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DispatchScorerTest {

    private DispatchScorer scorer;
    private List<GeoPoint> zones;

    @BeforeEach
    void setUp() {
        zones = List.of(
                new GeoPoint(51.50, -0.12),
                new GeoPoint(51.51, -0.10),
                new GeoPoint(51.52, -0.08)
        );
        CoverageModel coverage = new CoverageModel(10.0, zones);
        scorer = new DispatchScorer(coverage);
    }

    @Test
    void lowerEtaGivesBetterScore() {
        var incident = incident(Severity.EMERGENCY);
        var unit = unit(new GeoPoint(51.50, -0.12));
        var now = Instant.now();
        var units = List.of(unit);

        double score100s = scorer.score(unit, incident, 100, now, units);
        double score500s = scorer.score(unit, incident, 500, now, units);

        assertTrue(score100s < score500s, "Lower ETA should give lower (better) score");
    }

    @Test
    void alsUnitBetterForAlsRequiredIncident() {
        var incident = alsIncident();
        var now = Instant.now();

        var alsUnit = unit(UnitType.ALS, new GeoPoint(51.50, -0.12));
        var blsUnit = unit(UnitType.BLS, new GeoPoint(51.50, -0.12));
        var units = List.of(alsUnit, blsUnit);

        double alsScore = scorer.score(alsUnit, incident, 300, now, units);
        double blsScore = scorer.score(blsUnit, incident, 300, now, units);

        assertTrue(alsScore < blsScore, "ALS unit should score better for ALS-required incident");
    }

    @Test
    void breakdownComponentsSumToTotal() {
        var incident = incident(Severity.EMERGENCY);
        var unit = unit(new GeoPoint(51.50, -0.12));
        var now = Instant.now();
        var units = List.of(unit);

        var bd = scorer.breakdown(unit, incident, 300, now, units);
        var params = ScorerParams.defaults();
        double expected = params.etaWeight() * bd.etaComponent()
                + params.capabilityWeight() * bd.capabilityComponent()
                + params.fatigueWeight() * bd.fatigueComponent()
                + params.coverageWeight() * bd.coverageComponent()
                + params.stalePenaltyWeight() * bd.stalenessComponent();

        assertEquals(expected, bd.totalScore(), 0.0001);
    }

    private IncidentSnapshot incident(Severity severity) {
        return new IncidentSnapshot(UUID.randomUUID(),
                new GeoPoint(51.51, -0.11), severity, ClinicalNeed.GENERAL,
                false, IncidentStatus.TRIAGED, Instant.now());
    }

    private IncidentSnapshot alsIncident() {
        return new IncidentSnapshot(UUID.randomUUID(),
                new GeoPoint(51.51, -0.11), Severity.CRITICAL, ClinicalNeed.CARDIAC,
                true, IncidentStatus.TRIAGED, Instant.now());
    }

    private UnitSnapshot unit(GeoPoint pos) {
        return unit(UnitType.ALS, pos);
    }

    private UnitSnapshot unit(UnitType type, GeoPoint pos) {
        return new UnitSnapshot(UUID.randomUUID(), "A1", type,
                UnitStatus.AVAILABLE, pos, Instant.now(),
                Instant.now().minusSeconds(3600), UUID.randomUUID(), pos);
    }
}
