package com.h8.ems.incident.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.h8.ems.common.model.ClinicalNeed;
import com.h8.ems.common.model.IncidentStatus;
import com.h8.ems.common.model.Severity;
import com.h8.ems.contracts.dto.CreateIncidentRequest;
import com.h8.ems.incident.model.IncidentEntity;
import com.h8.ems.incident.model.OutboxEventEntity;
import com.h8.ems.incident.repository.IncidentRepository;
import com.h8.ems.incident.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private OutboxRepository outboxRepository;

    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        incidentService = new IncidentService(incidentRepository, outboxRepository, objectMapper);
    }

    @Test
    void createIncidentPersistsIncidentAndOutboxEvent() {
        CreateIncidentRequest req = new CreateIncidentRequest(
                51.50, -0.12, "CRITICAL", "CARDIAC", true, "+442079460000"
        );

        when(incidentRepository.save(any(IncidentEntity.class))).thenAnswer(invocation -> {
            IncidentEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        IncidentEntity created = incidentService.createIncident(req);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(Severity.CRITICAL, created.getSeverity());
        assertEquals(ClinicalNeed.CARDIAC, created.getNeed());
        assertTrue(created.isRequiresAls());
        assertEquals(IncidentStatus.RECEIVED, created.getStatus());
        assertNotNull(created.getReceivedAt());
        assertNotNull(created.getLocation());
        assertNotNull(created.getCallerHash());
        assertNotEquals("+442079460000", created.getCallerHash(), "Raw caller phone must never be stored");
        assertEquals(64, created.getCallerHash().length(), "Caller hash must be 64-char hex");

        verify(incidentRepository, times(1)).save(any(IncidentEntity.class));

        ArgumentCaptor<OutboxEventEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxRepository, times(1)).save(outboxCaptor.capture());

        OutboxEventEntity outbox = outboxCaptor.getValue();
        assertEquals(created.getId(), outbox.getAggregateId());
        assertEquals("incident.events", outbox.getTopic());
        assertEquals(created.getId().toString(), outbox.getEventKey());
        assertNotNull(outbox.getPayload());
        assertTrue(outbox.getPayload().contains("INCIDENT_CREATED"));
        assertNull(outbox.getPublishedAt());
    }

    @Test
    void getIncidentReturnsEntityWhenFound() {
        UUID id = UUID.randomUUID();
        IncidentEntity entity = new IncidentEntity();
        entity.setId(id);

        when(incidentRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<IncidentEntity> result = incidentService.getIncident(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }
}
