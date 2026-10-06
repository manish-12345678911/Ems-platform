package com.h8.ems.common.scoring;

import com.h8.ems.common.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CoverageModelTest {

    private final List<GeoPoint> zones = List.of(
            new GeoPoint(51.50, -0.12),
            new GeoPoint(51.51, -0.10),
            new GeoPoint(51.52, -0.08),
            new GeoPoint(51.53, -0.06)
    );

    @Test
    void emptyZonesGivesFullCoverage() {
        var model = new CoverageModel(10.0, List.of());
        assertEquals(1.0, model.coverage(List.of()));
    }

    @Test
    void noUnitsGivesZeroCoverage() {
        var model = new CoverageModel(1.0, zones);
        assertEquals(0.0, model.coverage(List.of()));
    }

    @Test
    void addingUnitNeverDecreasesCoverage() {
        var model = new CoverageModel(5.0, zones);
        var u1 = makeUnit(new GeoPoint(51.50, -0.12));

        double cov1 = model.coverage(List.of(u1));

        var u2 = makeUnit(new GeoPoint(51.52, -0.08));
        double cov2 = model.coverage(List.of(u1, u2));

        assertTrue(cov2 >= cov1, "Adding a unit should not decrease coverage");
    }

    @Test
    void lossIfRemovedIsNonNegative() {
        var model = new CoverageModel(5.0, zones);
        var u1 = makeUnit(new GeoPoint(51.50, -0.12));
        var u2 = makeUnit(new GeoPoint(51.52, -0.08));
        var units = List.of(u1, u2);

        assertTrue(model.lossIfRemoved(u1, units) >= 0.0);
        assertTrue(model.lossIfRemoved(u2, units) >= 0.0);
    }

    @Test
    void coverageIfMovedReturnsCoverageWithNewPosition() {
        var model = new CoverageModel(2.0, zones);
        var u1 = makeUnit(new GeoPoint(51.50, -0.12));
        var units = List.of(u1);

        // Move to cover a different zone
        double moved = model.coverageIfMoved(units, u1, new GeoPoint(51.52, -0.08));
        assertTrue(moved >= 0.0 && moved <= 1.0);
    }

    private UnitSnapshot makeUnit(GeoPoint pos) {
        return new UnitSnapshot(UUID.randomUUID(), "U1", UnitType.ALS,
                UnitStatus.AVAILABLE, pos, Instant.now(),
                Instant.now(), UUID.randomUUID(), pos);
    }
}
