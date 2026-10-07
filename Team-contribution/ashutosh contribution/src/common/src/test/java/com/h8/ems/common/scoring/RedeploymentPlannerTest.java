package com.h8.ems.common.scoring;

import com.h8.ems.common.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RedeploymentPlannerTest {

    @Test
    void planRespectsMaxMoves() {
        var zones = List.of(
                new GeoPoint(51.50, -0.12),
                new GeoPoint(51.60, -0.10),
                new GeoPoint(51.70, -0.08),
                new GeoPoint(51.80, -0.06)
        );
        var model = new CoverageModel(5.0, zones);
        var planner = new RedeploymentPlanner(model);

        var units = List.of(
                makeUnit(new GeoPoint(51.50, -0.12)),
                makeUnit(new GeoPoint(51.50, -0.13)),
                makeUnit(new GeoPoint(51.50, -0.14))
        );

        var standbyPoints = List.of(
                new GeoPoint(51.60, -0.10),
                new GeoPoint(51.70, -0.08),
                new GeoPoint(51.80, -0.06)
        );

        var limits = new RedeploymentPlanner.PlannerLimits(2, 0.001, 10);
        var moves = planner.plan(units, standbyPoints, limits);

        assertTrue(moves.size() <= 2, "Should respect maxMoves=2");
    }

    @Test
    void planRespectsMinGain() {
        var zones = List.of(new GeoPoint(51.50, -0.12));
        var model = new CoverageModel(100.0, zones); // huge radius — already covered
        var planner = new RedeploymentPlanner(model);

        var units = List.of(makeUnit(new GeoPoint(51.50, -0.12)));
        var standbyPoints = List.of(new GeoPoint(51.51, -0.12));

        var limits = new RedeploymentPlanner.PlannerLimits(5, 0.5, 10);
        var moves = planner.plan(units, standbyPoints, limits);

        assertTrue(moves.isEmpty(), "No move should exceed minGain=0.5 when already covered");
    }

    @Test
    void allMovesHavePositiveCoverageGain() {
        var zones = List.of(
                new GeoPoint(51.50, -0.12),
                new GeoPoint(51.60, -0.10),
                new GeoPoint(51.70, -0.08)
        );
        var model = new CoverageModel(5.0, zones);
        var planner = new RedeploymentPlanner(model);

        var units = List.of(
                makeUnit(new GeoPoint(51.50, -0.12)),
                makeUnit(new GeoPoint(51.50, -0.13))
        );
        var standbyPoints = List.of(
                new GeoPoint(51.60, -0.10),
                new GeoPoint(51.70, -0.08)
        );
        var limits = RedeploymentPlanner.PlannerLimits.defaults();
        var moves = planner.plan(units, standbyPoints, limits);

        for (var move : moves) {
            assertTrue(move.coverageGain() > 0, "Each move should have positive coverage gain");
        }
    }

    private UnitSnapshot makeUnit(GeoPoint pos) {
        return new UnitSnapshot(UUID.randomUUID(), "U1", UnitType.ALS,
                UnitStatus.AVAILABLE, pos, Instant.now(),
                Instant.now(), UUID.randomUUID(), pos);
    }
}
