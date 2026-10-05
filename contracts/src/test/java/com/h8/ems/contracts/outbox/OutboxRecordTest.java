package com.h8.ems.contracts.outbox;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OutboxRecordTest {

    @Test
    void createsOutboxRecordCorrectly() {
        UUID id = UUID.randomUUID();
        UUID aggId = UUID.randomUUID();
        Instant now = Instant.now();

        OutboxRecord rec = new OutboxRecord(id, aggId, "incident.events", aggId.toString(), "{}", now);

        assertEquals(id, rec.id());
        assertEquals(aggId, rec.aggregateId());
        assertEquals("incident.events", rec.topic());
        assertEquals(aggId.toString(), rec.eventKey());
        assertEquals("{}", rec.payload());
        assertEquals(now, rec.createdAt());
        assertNull(rec.publishedAt());
    }
}
