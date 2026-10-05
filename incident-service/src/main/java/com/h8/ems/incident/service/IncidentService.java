package com.h8.ems.incident.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h8.ems.common.model.ClinicalNeed;
import com.h8.ems.common.model.IncidentStatus;
import com.h8.ems.common.model.Severity;
import com.h8.ems.contracts.dto.CreateIncidentRequest;
import com.h8.ems.contracts.events.IncidentEvent;
import com.h8.ems.incident.model.IncidentEntity;
import com.h8.ems.incident.model.OutboxEventEntity;
import com.h8.ems.incident.repository.IncidentRepository;
import com.h8.ems.incident.repository.OutboxRepository;
import com.h8.ems.incident.util.CallerHashUtil;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing incident lifecycle and transactional outbox publishing.
 */
@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public IncidentService(IncidentRepository incidentRepository,
                           OutboxRepository outboxRepository,
                           ObjectMapper objectMapper) {
        this.incidentRepository = incidentRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Atomically creates an incident and writes an outbox event in the same transaction.
     */
    @Transactional
    public IncidentEntity createIncident(CreateIncidentRequest req) {
        Instant now = Instant.now();
        IncidentEntity entity = new IncidentEntity();
        entity.setReceivedAt(now);
        // PostGIS Point coordinates are (lon, lat)
        entity.setLocation(geometryFactory.createPoint(new Coordinate(req.lon(), req.lat())));
        entity.setSeverity(Severity.valueOf(req.severity().toUpperCase()));
        entity.setNeed(ClinicalNeed.valueOf(req.need().toUpperCase()));
        entity.setRequiresAls(req.requiresAls());
        entity.setStatus(IncidentStatus.RECEIVED);

        // Salted hash of caller phone (Rule #6: never store raw phone/PII)
        entity.setCallerHash(CallerHashUtil.hashPhoneNumber(req.callerHash()));

        IncidentEntity saved = incidentRepository.save(entity);

        // Transactional outbox event creation (Rule #4)
        java.util.Map<String, Object> eventBody = java.util.Map.of(
                "incidentId", saved.getId().toString(),
                "lat", req.lat(),
                "lon", req.lon(),
                "severity", saved.getSeverity().name(),
                "need", saved.getNeed().name(),
                "requiresAls", saved.isRequiresAls(),
                "status", saved.getStatus().name(),
                "receivedAt", saved.getReceivedAt().toString()
        );

        IncidentEvent event = new IncidentEvent(
                UUID.randomUUID(),
                "INCIDENT_CREATED",
                saved.getId(),
                now,
                serializeJson(eventBody)
        );

        OutboxEventEntity outbox = new OutboxEventEntity();
        outbox.setAggregateId(saved.getId());
        outbox.setTopic("incident.events");
        outbox.setEventKey(saved.getId().toString());
        outbox.setPayload(serializeJson(event));
        outbox.setCreatedAt(now);

        outboxRepository.save(outbox);

        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<IncidentEntity> getIncident(UUID id) {
        return incidentRepository.findById(id);
    }

    private String serializeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }
}
