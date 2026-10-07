package com.h8.ems.contracts.dto;

import java.util.UUID;

/**
 * REST DTO representing a nearby unit returned from tracking-service.
 */
public record NearbyUnitResponse(
        UUID unitId,
        double distanceKm,
        double lat,
        double lon,
        long lastSeenEpochMs
) {
}
