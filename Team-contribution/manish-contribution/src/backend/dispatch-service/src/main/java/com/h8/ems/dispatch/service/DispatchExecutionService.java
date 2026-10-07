package com.h8.ems.dispatch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h8.ems.contracts.dto.DispatchRequest;
import com.h8.ems.contracts.dto.DispatchResponse;
import com.h8.ems.contracts.dto.RejectRequest;
import com.h8.ems.contracts.events.DispatchDecision;
import com.h8.ems.contracts.events.UnitStatusEvent;
import com.h8.ems.dispatch.exception.UnitNotAvailableException;
import com.h8.ems.dispatch.model.AmbulanceUnitEntity;
import com.h8.ems.dispatch.model.AssignmentEntity;
import com.h8.ems.dispatch.model.OutboxEventEntity;
import com.h8.ems.dispatch.repository.AmbulanceUnitRepository;
import com.h8.ems.dispatch.repository.AssignmentRepository;
import com.h8.ems.dispatch.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service handling unit dispatch execution, dispatcher override, and crew reject workflows.
 * Enforces atomic conditional updates (Hard Rule #3) and transactional outbox events (Hard Rule #4).
 */
@Service
public class DispatchExecutionService {

    private static final Logger log = LoggerFactory.getLogger(DispatchExecutionService.class);

    private final AmbulanceUnitRepository unitRepository;
    private final AssignmentRepository assignmentRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public DispatchExecutionService(AmbulanceUnitRepository unitRepository,
                                    AssignmentRepository assignmentRepository,
                                    OutboxRepository outboxRepository,
                                    ObjectMapper objectMapper) {
        this.unitRepository = unitRepository;
        this.assignmentRepository = assignmentRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes dispatch assignment:
     * 1. Conditional SQL UPDATE (Hard Rule #3)
     * 2. Insert AssignmentEntity
     * 3. Insert OutboxEvent for dispatch.decisions and unit.status (Hard Rule #4)
     */
    @Transactional
    public DispatchResponse dispatch(DispatchRequest req) {
        if (req.incidentId() == null || req.unitId() == null) {
            throw new IllegalArgumentException("incidentId and unitId must not be null");
        }

        // 1. Atomic conditional update (Hard Rule #3)
        int updated = unitRepository.reserveIfAvailable(req.unitId());
        if (updated == 0) {
            log.warn("Unit reservation failed for unit {}: unit is no longer AVAILABLE", req.unitId());
            throw new UnitNotAvailableException("Unit " + req.unitId() + " is not available or already dispatched");
        }

        AmbulanceUnitEntity unit = unitRepository.findById(req.unitId())
                .orElseThrow(() -> new IllegalStateException("Unit not found: " + req.unitId()));

        Instant now = Instant.now();
        String snapshotJson = serializeRankedSnapshot(req.rankedCandidates());
        String chosenBy = req.chosenBy() != null ? req.chosenBy() : "AUTO";

        // 2. Persist assignment
        AssignmentEntity assignment = new AssignmentEntity(
                UUID.randomUUID(),
                req.incidentId(),
                unit,
                snapshotJson,
                chosenBy,
                now,
                false
        );
        assignmentRepository.save(assignment);

        // 3. Outbox event: dispatch.decisions
        DispatchDecision decision = new DispatchDecision(
                UUID.randomUUID(),
                req.incidentId(),
                req.unitId(),
                chosenBy,
                req.rankedCandidates() != null ? req.rankedCandidates() : List.of(),
                now
        );
        saveOutboxEvent(req.incidentId(), "dispatch.decisions", req.incidentId().toString(), decision, now);

        // 4. Outbox event: unit.status
        UnitStatusEvent statusEvent = new UnitStatusEvent(
                UUID.randomUUID(),
                req.unitId(),
                "AVAILABLE",
                "DISPATCHED",
                now
        );
        saveOutboxEvent(req.unitId(), "unit.status", req.unitId().toString(), statusEvent, now);

        log.info("Dispatched unit {} to incident {} by {}", req.unitId(), req.incidentId(), chosenBy);
        return new DispatchResponse(assignment.getId(), req.incidentId(), req.unitId(), "DISPATCHED", now);
    }

    /**
     * Dispatcher override workflow with mandatory override reason.
     */
    @Transactional
    public DispatchResponse override(DispatchRequest req) {
        if (req.overrideReason() == null || req.overrideReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Dispatcher override requires a valid overrideReason");
        }
        DispatchRequest overrideReq = new DispatchRequest(
                req.incidentId(),
                req.unitId(),
                "DISPATCHER",
                req.overrideReason(),
                req.rankedCandidates()
        );
        return dispatch(overrideReq);
    }

    /**
     * Crew reject workflow:
     * 1. Frees unit back to AVAILABLE (Hard Rule #3)
     * 2. Marks previous assignment as rejected
     * 3. Publishes unit.status outbox event (DISPATCHED -> AVAILABLE)
     */
    @Transactional
    public void reject(RejectRequest req) {
        if (req.incidentId() == null || req.unitId() == null) {
            throw new IllegalArgumentException("incidentId and unitId must not be null");
        }

        int freed = unitRepository.freeUnit(req.unitId());
        if (freed == 0) {
            log.warn("Attempted to free unit {} but unit status was not DISPATCHED", req.unitId());
        }

        assignmentRepository.findFirstByIncidentIdAndUnitIdOrderByDecidedAtDesc(req.incidentId(), req.unitId())
                .ifPresent(assignment -> {
                    assignment.setRejected(true);
                    assignmentRepository.save(assignment);
                });

        Instant now = Instant.now();
        UnitStatusEvent statusEvent = new UnitStatusEvent(
                UUID.randomUUID(),
                req.unitId(),
                "DISPATCHED",
                "AVAILABLE",
                now
        );
        saveOutboxEvent(req.unitId(), "unit.status", req.unitId().toString(), statusEvent, now);

        log.info("Unit {} rejected dispatch for incident {} (reason: {})", req.unitId(), req.incidentId(), req.reason());
    }

    private void saveOutboxEvent(UUID aggregateId, String topic, String eventKey, Object event, Instant now) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEventEntity outbox = new OutboxEventEntity(
                    UUID.randomUUID(),
                    aggregateId,
                    topic,
                    eventKey,
                    payload,
                    now,
                    null
            );
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event for topic " + topic, e);
        }
    }

    private String serializeRankedSnapshot(Object snapshot) {
        if (snapshot == null) return null;
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize ranked snapshot JSON: {}", e.getMessage());
            return null;
        }
    }
}
