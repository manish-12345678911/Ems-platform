package com.h8.ems.contracts.outbox;

import java.time.Instant;
import java.util.UUID;

/**
 * Common record representing an outbox event.
 */
public record OutboxRecord(
        UUID id,
        UUID aggregateId,
        String topic,
        String eventKey,
        String payload,
        Instant createdAt,
        Instant publishedAt
) {
    public OutboxRecord(UUID id, UUID aggregateId, String topic, String eventKey, String payload, Instant createdAt) {
        this(id, aggregateId, topic, eventKey, payload, createdAt, null);
    }
}
