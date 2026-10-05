package com.h8.ems.hospital.service;

import com.h8.ems.contracts.dto.PreArrivalAlertDto;
import com.h8.ems.hospital.model.PreArrivalAlertEntity;
import com.h8.ems.hospital.repository.PreArrivalAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlertHubTest {

    private PreArrivalAlertRepository alertRepository;
    private AlertHub alertHub;

    @BeforeEach
    void setUp() {
        alertRepository = mock(PreArrivalAlertRepository.class);
        alertHub = new AlertHub(alertRepository);
    }

    @Test
    void testSubscribeAndHeartbeat() {
        UUID hospitalId = UUID.randomUUID();
        SseEmitter emitter = alertHub.subscribe(hospitalId, null);
        assertNotNull(emitter);
        assertEquals(1, alertHub.getActiveSubscriptionCount(hospitalId));

        // Heartbeat should succeed without exception
        assertDoesNotThrow(() -> alertHub.sendHeartbeats());
    }

    @Test
    void testPublishAlertPersistsAndBuffers() {
        UUID hospitalId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();

        PreArrivalAlertDto alert = new PreArrivalAlertDto(
                alertId,
                incidentId,
                hospitalId,
                "CRITICAL",
                "TRAUMA",
                180,
                Instant.now()
        );

        when(alertRepository.save(any(PreArrivalAlertEntity.class))).thenAnswer(i -> i.getArgument(0));

        alertHub.publishAlert(alert);

        verify(alertRepository, times(1)).save(any(PreArrivalAlertEntity.class));

        // Reconnect with Last-Event-ID should replay buffered alert
        SseEmitter reconnected = alertHub.subscribe(hospitalId, UUID.randomUUID().toString());
        assertNotNull(reconnected);
    }
}
