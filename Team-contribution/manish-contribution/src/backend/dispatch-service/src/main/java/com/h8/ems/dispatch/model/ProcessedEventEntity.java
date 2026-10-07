package com.h8.ems.dispatch.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity for tracking processed events to ensure idempotent consumption (Hard Rule #4).
 */
@Entity
@Table(name = "processed_event", schema = "dispatch")
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

    public ProcessedEventEntity() {}

    public ProcessedEventEntity(String consumer, UUID eventId, Instant processedAt) {
        this.consumer = consumer;
        this.eventId = eventId;
        this.processedAt = processedAt != null ? processedAt : Instant.now();
    }

    public String getConsumer() { return consumer; }
    public void setConsumer(String consumer) { this.consumer = consumer; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
