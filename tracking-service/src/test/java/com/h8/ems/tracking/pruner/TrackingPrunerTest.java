package com.h8.ems.tracking.pruner;

import com.h8.ems.contracts.events.UnitStatusEvent;
import com.h8.ems.tracking.service.TrackingRedisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingPrunerTest {

    @Mock
    private TrackingRedisService trackingRedisService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void doesNothingWhenNoUnitsExpired() {
        when(trackingRedisService.pruneExpiredUnits(anyLong())).thenReturn(Collections.emptyList());

        TrackingPruner pruner = new TrackingPruner(trackingRedisService, kafkaTemplate, 90);
        pruner.pruneExpiredUnits();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void emitsOfflineStatusEventForPrunedUnits() {
        UUID unitId = UUID.randomUUID();
        when(trackingRedisService.pruneExpiredUnits(90)).thenReturn(List.of(unitId));
        when(kafkaTemplate.send(eq("unit.status"), eq(unitId.toString()), any(UnitStatusEvent.class)))
                .thenReturn(new CompletableFuture<>());

        TrackingPruner pruner = new TrackingPruner(trackingRedisService, kafkaTemplate, 90);
        pruner.pruneExpiredUnits();

        ArgumentCaptor<UnitStatusEvent> captor = ArgumentCaptor.forClass(UnitStatusEvent.class);
        verify(kafkaTemplate, times(1)).send(eq("unit.status"), eq(unitId.toString()), captor.capture());

        UnitStatusEvent event = captor.getValue();
        assertEquals(unitId, event.unitId());
        assertEquals("OFFLINE", event.to());
        assertEquals("AVAILABLE", event.from());
    }
}
