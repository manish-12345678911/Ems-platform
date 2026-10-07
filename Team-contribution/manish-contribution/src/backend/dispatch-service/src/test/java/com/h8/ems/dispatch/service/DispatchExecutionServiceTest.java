package com.h8.ems.dispatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h8.ems.common.model.UnitStatus;
import com.h8.ems.common.model.UnitType;
import com.h8.ems.contracts.dto.DispatchRequest;
import com.h8.ems.contracts.dto.DispatchResponse;
import com.h8.ems.contracts.dto.RejectRequest;
import com.h8.ems.dispatch.exception.UnitNotAvailableException;
import com.h8.ems.dispatch.model.AmbulanceUnitEntity;
import com.h8.ems.dispatch.model.AssignmentEntity;
import com.h8.ems.dispatch.model.OutboxEventEntity;
import com.h8.ems.dispatch.repository.AmbulanceUnitRepository;
import com.h8.ems.dispatch.repository.AssignmentRepository;
import com.h8.ems.dispatch.repository.OutboxRepository;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchExecutionServiceTest {

    @Mock
    private AmbulanceUnitRepository unitRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private OutboxRepository outboxRepository;

    private DispatchExecutionService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new DispatchExecutionService(
                unitRepository, assignmentRepository, outboxRepository, objectMapper
        );
    }

    @Test
    void dispatchSuccessWhenUnitIsAvailable() {
        UUID incidentId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();

        // Simulate that conditional update succeeds (1 row affected)
        when(unitRepository.reserveIfAvailable(unitId)).thenReturn(1);

        AmbulanceUnitEntity unit = new AmbulanceUnitEntity();
        unit.setId(unitId);
        unit.setCallSign("ALS-1");
        unit.setType(UnitType.ALS);
        unit.setStatus(UnitStatus.DISPATCHED);
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(unit));

        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DispatchRequest req = new DispatchRequest(incidentId, unitId, "AUTO");
        DispatchResponse resp = service.dispatch(req);

        assertNotNull(resp);
        assertEquals(incidentId, resp.incidentId());
        assertEquals(unitId, resp.unitId());
        assertEquals("DISPATCHED", resp.status());
        assertNotNull(resp.assignmentId());

        // Verify conditional update was called (Hard Rule #3)
        verify(unitRepository, times(1)).reserveIfAvailable(unitId);

        // Verify assignment was persisted
        verify(assignmentRepository, times(1)).save(any(AssignmentEntity.class));

        // Verify two outbox events were created: dispatch.decisions + unit.status
        ArgumentCaptor<OutboxEventEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxRepository, times(2)).save(outboxCaptor.capture());

        List<String> topics = outboxCaptor.getAllValues().stream()
                .map(OutboxEventEntity::getTopic)
                .toList();
        assertTrue(topics.contains("dispatch.decisions"));
        assertTrue(topics.contains("unit.status"));
    }

    @Test
    void dispatchThrowsWhenUnitNotAvailable() {
        UUID incidentId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();

        // Conditional update returns 0 → unit is not available
        when(unitRepository.reserveIfAvailable(unitId)).thenReturn(0);

        DispatchRequest req = new DispatchRequest(incidentId, unitId, "AUTO");

        assertThrows(UnitNotAvailableException.class, () -> service.dispatch(req));

        // No assignment or outbox events should be created on failure
        verify(assignmentRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void dispatchThrowsOnNullInputs() {
        assertThrows(IllegalArgumentException.class, () ->
                service.dispatch(new DispatchRequest(null, UUID.randomUUID(), "AUTO")));
        assertThrows(IllegalArgumentException.class, () ->
                service.dispatch(new DispatchRequest(UUID.randomUUID(), null, "AUTO")));
    }

    @Test
    void overrideRequiresReason() {
        UUID incidentId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();

        // No override reason → should throw
        DispatchRequest noReasonReq = new DispatchRequest(incidentId, unitId, "DISPATCHER", null, List.of());
        assertThrows(IllegalArgumentException.class, () -> service.override(noReasonReq));

        // Empty override reason → should throw
        DispatchRequest emptyReasonReq = new DispatchRequest(incidentId, unitId, "DISPATCHER", "  ", List.of());
        assertThrows(IllegalArgumentException.class, () -> service.override(emptyReasonReq));
    }

    @Test
    void rejectFreesUnitAndMarksAssignment() {
        UUID incidentId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();

        when(unitRepository.freeUnit(unitId)).thenReturn(1);

        AssignmentEntity existing = new AssignmentEntity();
        existing.setId(UUID.randomUUID());
        existing.setIncidentId(incidentId);
        existing.setRejected(false);
        when(assignmentRepository.findFirstByIncidentIdAndUnitIdOrderByDecidedAtDesc(incidentId, unitId))
                .thenReturn(Optional.of(existing));
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.reject(new RejectRequest(incidentId, unitId, "Mechanical failure"));

        verify(unitRepository, times(1)).freeUnit(unitId);
        verify(assignmentRepository, times(1)).save(existing);
        assertTrue(existing.isRejected());

        // Outbox: unit.status (DISPATCHED -> AVAILABLE)
        ArgumentCaptor<OutboxEventEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxRepository, times(1)).save(outboxCaptor.capture());
        assertEquals("unit.status", outboxCaptor.getValue().getTopic());
    }
}
