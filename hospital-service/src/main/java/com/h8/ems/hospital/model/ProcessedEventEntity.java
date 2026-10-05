package com.h8.ems.hospital.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "processed_event", schema = "hospital")
@IdClass(ProcessedEventEntity.ProcessedEventId.class)
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

    public String getConsumer() { return consumer; }
    public void setConsumer(String consumer) { this.consumer = consumer; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    public static class ProcessedEventId implements Serializable {
        private String consumer;
        private UUID eventId;

        public ProcessedEventId() {}

        public ProcessedEventId(String consumer, UUID eventId) {
            this.consumer = consumer;
            this.eventId = eventId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProcessedEventId that = (ProcessedEventId) o;
            return Objects.equals(consumer, that.consumer) && Objects.equals(eventId, that.eventId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(consumer, eventId);
        }
    }
}
