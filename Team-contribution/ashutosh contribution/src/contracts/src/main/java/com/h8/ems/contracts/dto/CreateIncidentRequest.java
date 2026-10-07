package com.h8.ems.contracts.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * REST DTO for creating a new incident.
 */
public record CreateIncidentRequest(
        double lat,
        double lon,
        String severity,
        String need,
        boolean requiresAls,
        String callerHash
) {
}
