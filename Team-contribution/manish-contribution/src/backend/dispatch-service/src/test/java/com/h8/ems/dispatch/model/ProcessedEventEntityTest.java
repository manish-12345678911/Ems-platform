package com.h8.ems.dispatch.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProcessedEventEntityTest {

    @Test
    void constructorAndGetters() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();

        ProcessedEventEntity pe = new ProcessedEventEntity("dispatch-consumer", eventId, now);

        assertEquals("dispatch-consumer", pe.getConsumer());
        assertEquals(eventId, pe.getEventId());
        assertEquals(now, pe.getProcessedAt());
    }

    @Test
    void compositeIdEquality() {
        UUID eventId = UUID.randomUUID();
        ProcessedEventId id1 = new ProcessedEventId("consumer1", eventId);
        ProcessedEventId id2 = new ProcessedEventId("consumer1", eventId);
        ProcessedEventId id3 = new ProcessedEventId("consumer2", eventId);

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1, id3);
    }
}
