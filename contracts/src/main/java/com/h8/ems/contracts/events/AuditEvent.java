package com.h8.ems.contracts.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event for audit trail.
 * Topic: audit.events, Key: incidentId
 */
public record AuditEvent(
        UUID eventId,
        String kind,
        String actor,
        String payload,
        Instant at,
        int schemaVersion
) {
    public AuditEvent(UUID eventId, String kind, String actor, String payload, Instant at) {
        this(eventId, kind, actor, payload, at, 1);
    }
}
