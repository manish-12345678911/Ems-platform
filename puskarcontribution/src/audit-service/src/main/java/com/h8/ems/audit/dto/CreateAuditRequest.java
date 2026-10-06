package com.h8.ems.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateAuditRequest(
        UUID eventId,
        String kind,
        String actor,
        String payload,
        Instant at
) {
    public CreateAuditRequest {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (at == null) {
            at = Instant.now();
        }
    }
}
