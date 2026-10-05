package com.h8.ems.contracts.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event for incident state changes.
 * Topic: incident.events, Key: incidentId
 */
public record IncidentEvent(
        UUID eventId,
        String type,
        UUID incidentId,
        Instant at,
        String body,
        int schemaVersion
) {
    public IncidentEvent(UUID eventId, String type, UUID incidentId, Instant at, String body) {
        this(eventId, type, incidentId, at, body, 1);
    }
}
