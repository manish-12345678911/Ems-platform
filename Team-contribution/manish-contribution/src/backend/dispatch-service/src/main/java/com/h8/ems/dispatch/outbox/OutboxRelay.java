package com.h8.ems.dispatch.outbox;

import com.h8.ems.dispatch.model.OutboxEventEntity;
import com.h8.ems.dispatch.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled worker polling unpublished outbox records and publishing to Kafka.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:500}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventEntity> pending = outboxRepository.findUnpublishedEvents();
        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEventEntity event : pending) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                event.setPublishedAt(Instant.now());
                                outboxRepository.save(event);
                                log.debug("Published outbox event {} to topic {}", event.getId(), event.getTopic());
                            } else {
                                log.warn("Failed to publish outbox event {}: {}", event.getId(), ex.getMessage());
                            }
                        });
            } catch (Exception e) {
                log.warn("Error sending outbox event {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}
