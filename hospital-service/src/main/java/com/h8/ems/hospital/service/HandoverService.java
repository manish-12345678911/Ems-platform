package com.h8.ems.hospital.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h8.ems.contracts.dto.HandoverRequest;
import com.h8.ems.hospital.model.OutboxEventEntity;
import com.h8.ems.hospital.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service managing patient handover completion at the hospital.
 * Records timestamp and publishes handover event to Kafka outbox.
 */
@Service
public class HandoverService {

    private static final Logger log = LoggerFactory.getLogger(HandoverService.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public HandoverService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> recordHandover(UUID hospitalId, HandoverRequest request) {
        Instant handedOverAt = Instant.now();

        Map<String, Object> payloadMap = Map.of(
                "hospitalId", hospitalId.toString(),
                "incidentId", request.incidentId().toString(),
                "unitId", request.unitId().toString(),
                "notes", request.notes() != null ? request.notes() : "",
                "handedOverAt", handedOverAt.toString()
        );

        try {
            outboxRepository.save(new OutboxEventEntity(
                    null,
                    request.incidentId(),
                    "unit.handover",
                    request.unitId().toString(),
                    objectMapper.writeValueAsString(payloadMap),
                    handedOverAt,
                    null
            ));
        } catch (JsonProcessingException e) {
            log.error("Failed to write outbox event for handover: {}", e.getMessage());
        }

        return payloadMap;
    }
}
