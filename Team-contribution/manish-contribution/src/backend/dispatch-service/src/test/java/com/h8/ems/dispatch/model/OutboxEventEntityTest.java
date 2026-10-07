package com.h8.ems.dispatch.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventEntityTest {

    @Test
    void constructorAndGetters() {
        UUID id = UUID.randomUUID();
        UUID aggId = UUID.randomUUID();
        Instant now = Instant.now();

        OutboxEventEntity e = new OutboxEventEntity(
                id, aggId, "dispatch.decisions", "key1", "{}", now, null
        );

        assertEquals(id, e.getId());
        assertEquals(aggId, e.getAggregateId());
        assertEquals("dispatch.decisions", e.getTopic());
        assertEquals("key1", e.getEventKey());
        assertEquals("{}", e.getPayload());
        assertEquals(now, e.getCreatedAt());
        assertNull(e.getPublishedAt());
    }

    @Test
    void unpublishedThenPublished() {
        OutboxEventEntity e = new OutboxEventEntity();
        e.setId(UUID.randomUUID());
        e.setTopic("unit.status");
        assertNull(e.getPublishedAt());

        Instant pubTime = Instant.now();
        e.setPublishedAt(pubTime);
        assertEquals(pubTime, e.getPublishedAt());
    }
}
