package com.h8.ems.audit.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Idempotent consumer deduplication table.
 */
@Entity
@Table(name = "processed_event", schema = "audit")
@IdClass(ProcessedEventId.class)
public class ProcessedEventEntity {

    @Id
    @Column(name = "consumer", nullable = false, length = 64)
    private String consumer;

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    public ProcessedEventEntity() {
    }

    public ProcessedEventEntity(String consumer, UUID eventId) {
        this.consumer = consumer;
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public String getConsumer() {
        return consumer;
    }

    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
