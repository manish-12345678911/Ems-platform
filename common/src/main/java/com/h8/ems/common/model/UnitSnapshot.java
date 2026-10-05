package com.h8.ems.common.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable snapshot of an ambulance unit at a point in time.
 * Services map their JPA entities to/from this snapshot for use with scorers.
 * This keeps common free of JPA annotations.
 */
public record UnitSnapshot(
        UUID id,
        String callSign,
        UnitType type,
        UnitStatus status,
        GeoPoint position,
        Instant positionAt,
        Instant shiftStart,
        UUID homeStationId,
        GeoPoint homeStationLocation
) {
    /**
     * Returns true if the position data is stale (older than ttlSeconds).
     */
    public boolean isPositionStale(Instant now, long ttlSeconds) {
        return positionAt == null || positionAt.plusSeconds(ttlSeconds).isBefore(now);
    }

    /**
     * Hours since shift started, for fatigue scoring.
     */
    public double hoursOnShift(Instant now) {
        if (shiftStart == null) return 0.0;
        return (now.toEpochMilli() - shiftStart.toEpochMilli()) / 3_600_000.0;
    }
}
