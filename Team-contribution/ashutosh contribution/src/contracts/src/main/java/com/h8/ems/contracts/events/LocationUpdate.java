package com.h8.ems.contracts.events;

import java.util.UUID;

/**
 * Kafka event for GPS location updates from crew devices.
 * Topic: unit.location, Key: unitId
 * Lossy by design — latest wins.
 */
public record LocationUpdate(
        UUID unitId,
        double lat,
        double lon,
        long epochMs,
        double speed,
        int schemaVersion
) {
    public LocationUpdate(UUID unitId, double lat, double lon, long epochMs, double speed) {
        this(unitId, lat, lon, epochMs, speed, 1);
    }
}
