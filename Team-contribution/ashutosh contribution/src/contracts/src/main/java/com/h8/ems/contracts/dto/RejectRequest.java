package com.h8.ems.contracts.dto;

import java.util.UUID;

/**
 * Request DTO for crew rejecting a dispatch assignment.
 */
public record RejectRequest(
        UUID incidentId,
        UUID unitId,
        String reason
) {
}
