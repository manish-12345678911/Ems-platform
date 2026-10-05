package com.h8.ems.contracts.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event for hospital pre-arrival alerts.
 * Topic: hospital.alerts, Key: hospitalId
 */
public record PreArrivalAlert(
        UUID eventId,
        UUID incidentId,
        UUID hospitalId,
        String severity,
        String need,
        boolean requiresAls,
        int etaSeconds,
        Instant sentAt,
        int schemaVersion
) {
    public PreArrivalAlert(UUID eventId, UUID incidentId, UUID hospitalId,
                            String severity, String need, boolean requiresAls,
                            int etaSeconds, Instant sentAt) {
        this(eventId, incidentId, hospitalId, severity, need, requiresAls, etaSeconds, sentAt, 1);
    }
}
