package com.h8.ems.incident.outbox;

import com.h8.ems.incident.model.OutboxEventEntity;
import com.h8.ems.incident.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxRelay outboxRelay;

    @BeforeEach
    void setUp() {
        outboxRelay = new OutboxRelay(outboxRepository, kafkaTemplate);
    }

    @Test
    void doesNothingWhenNoPendingEvents() {
        when(outboxRepository.findUnpublishedEvents()).thenReturn(Collections.emptyList());

        outboxRelay.publishPendingEvents();

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void publishesPendingEventAndUpdatesPublishedAt() {
        UUID eventId = UUID.randomUUID();
        UUID aggId = UUID.randomUUID();
        OutboxEventEntity entity = new OutboxEventEntity(
                eventId, aggId, "incident.events", aggId.toString(), "{}", Instant.now(), null
        );

        when(outboxRepository.findUnpublishedEvents()).thenReturn(List.of(entity));

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("incident.events"), eq(aggId.toString()), eq("{}"))).thenReturn(future);

        outboxRelay.publishPendingEvents();

        verify(kafkaTemplate, times(1)).send("incident.events", aggId.toString(), "{}");
        assertNull(entity.getPublishedAt(), "publishedAt should not be set before future completes");

        // Simulate successful ACK from Kafka broker
        future.complete(null);

        assertNotNull(entity.getPublishedAt(), "publishedAt should be updated after ACK");
        verify(outboxRepository, times(1)).save(entity);
    }

    @Test
    void handlesKafkaFailureWithoutLosingEvent() {
        UUID eventId = UUID.randomUUID();
        UUID aggId = UUID.randomUUID();
        OutboxEventEntity entity = new OutboxEventEntity(
                eventId, aggId, "incident.events", aggId.toString(), "{}", Instant.now(), null
        );

        when(outboxRepository.findUnpublishedEvents()).thenReturn(List.of(entity));

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("incident.events"), eq(aggId.toString()), eq("{}"))).thenReturn(future);

        outboxRelay.publishPendingEvents();

        // Simulate Kafka broker failure / connection drop
        future.completeExceptionally(new RuntimeException("Kafka broker unavailable"));

        assertNull(entity.getPublishedAt(), "publishedAt must remain null on failure to allow retry");
        verify(outboxRepository, never()).save(entity);
    }
}
