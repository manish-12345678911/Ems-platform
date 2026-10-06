package com.h8.ems.audit.service;

import com.h8.ems.audit.dto.AuditLogDto;
import com.h8.ems.audit.dto.VerificationResult;
import com.h8.ems.audit.model.AuditLogEntity;
import com.h8.ems.audit.model.ProcessedEventEntity;
import com.h8.ems.audit.repository.AuditLogRepository;
import com.h8.ems.audit.repository.ProcessedEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuditServiceTest {

    private AuditLogRepository auditLogRepository;
    private ProcessedEventRepository processedEventRepository;
    private HashChainService hashChainService;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        processedEventRepository = mock(ProcessedEventRepository.class);
        hashChainService = new HashChainService();
        auditService = new AuditService(
                auditLogRepository,
                processedEventRepository,
                hashChainService,
                new SimpleMeterRegistry()
        );
    }

    @Test
    @DisplayName("Recording first audit event establishes genesis block")
    void testRecordFirstEventGenesis() {
        UUID eventId = UUID.randomUUID();
        when(processedEventRepository.existsByConsumerAndEventId("audit-service", eventId)).thenReturn(false);
        when(auditLogRepository.findTopByOrderBySeqDesc()).thenReturn(Optional.empty());

        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(invocation -> {
            AuditLogEntity e = invocation.getArgument(0);
            e.setSeq(1L);
            return e;
        });

        AuditLogDto result = auditService.recordEvent(
                eventId,
                "INCIDENT_CREATED",
                "dispatcher1",
                "{\"priority\":1}",
                Instant.now()
        );

        assertNotNull(result);
        assertEquals(1L, result.seq());
        assertEquals(eventId, result.eventId());
        assertEquals(HashChainService.GENESIS_HASH, result.prevHash());
        assertNotNull(result.hash());
        assertEquals(64, result.hash().length());

        verify(processedEventRepository).save(any(ProcessedEventEntity.class));
    }

    @Test
    @DisplayName("Duplicate eventId is deduplicated idempotently")
    void testDeduplicateOnEventId() {
        UUID eventId = UUID.randomUUID();
        when(processedEventRepository.existsByConsumerAndEventId("audit-service", eventId)).thenReturn(true);

        AuditLogEntity existing = new AuditLogEntity(
                eventId,
                "UNIT_ASSIGNED",
                "system",
                "{\"unitId\":\"AMB-101\"}",
                HashChainService.GENESIS_HASH,
                "somehash",
                Instant.now()
        );
        existing.setSeq(10L);
        when(auditLogRepository.findByEventId(eventId)).thenReturn(Optional.of(existing));

        AuditLogDto result = auditService.recordEvent(
                eventId,
                "UNIT_ASSIGNED",
                "system",
                "{\"unitId\":\"AMB-101\"}",
                Instant.now()
        );

        assertNotNull(result);
        assertEquals(10L, result.seq());
        // Verify no new insert occurred
        verify(auditLogRepository, never()).save(any(AuditLogEntity.class));
    }

    @Test
    @DisplayName("Integrity verification delegates to hash chain service")
    void testVerifyIntegrity() {
        when(auditLogRepository.findAllByOrderBySeqAsc()).thenReturn(List.of());

        VerificationResult result = auditService.verifyIntegrity();
        assertTrue(result.valid());
        assertEquals(0, result.totalEntries());
    }
}
