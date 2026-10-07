package com.h8.ems.contracts.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO after successfully dispatching a unit to an incident.
 */
public record DispatchResponse(
        UUID assignmentId,
        UUID incidentId,
        UUID unitId,
        String status,
        Instant dispatchedAt
) {
}
