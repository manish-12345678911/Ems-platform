package com.h8.ems.common.scoring;

import com.h8.ems.common.model.GeoPoint;
import com.h8.ems.common.model.UnitSnapshot;

import java.util.List;

/**
 * Coverage model: measures what fraction of demand zones are covered by at least one unit
 * within a threshold travel time/distance.
 *
 * Includes coverageIfMoved (correction #1 from architecture section 15).
 */
public final class CoverageModel {

    private final double coverageRadiusKm;
    private final List<GeoPoint> demandZoneCentroids;

    public CoverageModel(double coverageRadiusKm, List<GeoPoint> demandZoneCentroids) {
        this.coverageRadiusKm = coverageRadiusKm;
        this.demandZoneCentroids = demandZoneCentroids;
    }

    /**
     * Fraction of demand zones covered by at least one unit within radius.
     *
     * @param units available units
     * @return coverage ratio [0.0, 1.0]
     */
    public double coverage(List<UnitSnapshot> units) {
        if (demandZoneCentroids.isEmpty()) return 1.0;
        long covered = demandZoneCentroids.stream()
                .filter(zone -> units.stream()
                        .anyMatch(u -> u.position() != null
                                && u.position().distanceTo(zone) <= coverageRadiusKm))
                .count();
        return (double) covered / demandZoneCentroids.size();
    }

    /**
     * Coverage loss if the given unit is removed from the available pool.
     * Higher value = removing this unit hurts coverage more = it should be kept.
     *
     * @param unit      the unit to consider removing
     * @param available all currently available units
     * @return coverage drop [0.0, 1.0]
     */
    public double lossIfRemoved(UnitSnapshot unit, List<UnitSnapshot> available) {
        double currentCoverage = coverage(available);
        List<UnitSnapshot> without = available.stream()
                .filter(u -> !u.id().equals(unit.id()))
                .toList();
        double reducedCoverage = coverage(without);
        return Math.max(0.0, currentCoverage - reducedCoverage);
    }

    /**
     * Coverage if unit is moved from its current position to a standby point.
     * Per architecture correction #1: CoverageModel.coverageIfMoved was called
     * but never defined — now it is.
     *
     * @param units    available units
     * @param unit     the unit being moved
     * @param standby  the target standby point
     * @return coverage ratio after the hypothetical move
     */
    public double coverageIfMoved(List<UnitSnapshot> units, UnitSnapshot unit, GeoPoint standby) {
        List<UnitSnapshot> adjusted = units.stream()
                .map(u -> u.id().equals(unit.id())
                        ? new UnitSnapshot(u.id(), u.callSign(), u.type(), u.status(),
                        standby, u.positionAt(), u.shiftStart(),
                        u.homeStationId(), u.homeStationLocation())
                        : u)
                .toList();
        return coverage(adjusted);
    }
}
