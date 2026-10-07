package com.h8.ems.contracts.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event for unit status changes.
 * Topic: unit.status, Key: unitId
 */
public record UnitStatusEvent(
        UUID eventId,
        UUID unitId,
        String from,
        String to,
        Instant at,
        int schemaVersion
) {
    public UnitStatusEvent(UUID eventId, UUID unitId, String from, String to, Instant at) {
        this(eventId, unitId, from, to, at, 1);
    }
}
