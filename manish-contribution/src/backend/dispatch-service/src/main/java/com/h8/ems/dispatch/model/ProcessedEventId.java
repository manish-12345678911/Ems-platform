package com.h8.ems.dispatch.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ProcessedEventId implements Serializable {
    private String consumer;
    private UUID eventId;

    public ProcessedEventId() {}

    public ProcessedEventId(String consumer, UUID eventId) {
        this.consumer = consumer;
        this.eventId = eventId;
    }

    public String getConsumer() { return consumer; }
    public void setConsumer(String consumer) { this.consumer = consumer; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessedEventId that)) return false;
        return Objects.equals(consumer, that.consumer) && Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumer, eventId);
    }
}
