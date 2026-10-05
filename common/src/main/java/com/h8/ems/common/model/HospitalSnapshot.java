package com.h8.ems.common.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable snapshot of a hospital, including current capacity data.
 */
public record HospitalSnapshot(
        UUID id,
        String name,
        GeoPoint location,
        Set<ClinicalNeed> capabilities,
        int edBedsFree,
        int icuBedsFree,
        int ventilatorsFree,
        Instant capacityUpdatedAt
) {
    /**
     * Returns true if the capacity data is stale (older than ttlMinutes).
     */
    public boolean isCapacityStale(Instant now, int ttlMinutes) {
        if (capacityUpdatedAt == null) return true;
        return capacityUpdatedAt.plusSeconds((long) ttlMinutes * 60).isBefore(now);
    }

    /**
     * Returns true if the hospital can handle the given clinical need.
     */
    public boolean supports(ClinicalNeed need) {
        return capabilities != null && capabilities.contains(need);
    }

    /**
     * Estimated wait time based on bed occupancy (simple model).
     */
    public double estimatedWaitMinutes() {
        if (edBedsFree > 3) return 0.0;
        if (edBedsFree > 0) return 10.0;
        return 30.0; // full — significant wait
    }
}
