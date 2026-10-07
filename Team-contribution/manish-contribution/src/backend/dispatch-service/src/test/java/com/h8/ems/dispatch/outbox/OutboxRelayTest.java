package com.h8.ems.dispatch.outbox;

import com.h8.ems.dispatch.model.OutboxEventEntity;
import com.h8.ems.dispatch.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    void publishPendingEventsDoesNothingWhenNoPendingEvents() {
        when(outboxRepository.findUnpublishedEvents()).thenReturn(List.of());

        outboxRelay.publishPendingEvents();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void publishPendingEventsSendsToKafka() {
        UUID eventId = UUID.randomUUID();
        OutboxEventEntity event = new OutboxEventEntity(
                eventId, UUID.randomUUID(), "dispatch.decisions", "key1",
                "{\"test\":true}", Instant.now(), null
        );

        when(outboxRepository.findUnpublishedEvents()).thenReturn(List.of(event));

        // Simulate successful Kafka send
        CompletableFuture future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(eq("dispatch.decisions"), eq("key1"), eq("{\"test\":true}")))
                .thenReturn(future);

        outboxRelay.publishPendingEvents();

        verify(kafkaTemplate, times(1)).send("dispatch.decisions", "key1", "{\"test\":true}");
    }

    @Test
    void publishPendingEventsHandlesKafkaFailure() {
        OutboxEventEntity event = new OutboxEventEntity(
                UUID.randomUUID(), UUID.randomUUID(), "unit.status", "key2",
                "{}", Instant.now(), null
        );

        when(outboxRepository.findUnpublishedEvents()).thenReturn(List.of(event));

        // Simulate Kafka send throwing exception
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Kafka broker unavailable"));

        // Should not throw — just log the error
        assertDoesNotThrow(() -> outboxRelay.publishPendingEvents());
    }
}
