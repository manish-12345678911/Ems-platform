package com.h8.ems.common.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable snapshot of an incident at a point in time.
 */
public record IncidentSnapshot(
        UUID id,
        GeoPoint location,
        Severity severity,
        ClinicalNeed need,
        boolean requiresAls,
        IncidentStatus status,
        Instant receivedAt
) {
}
