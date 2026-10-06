package com.h8.ems.audit.service;

import com.h8.ems.audit.dto.AuditLogDto;
import com.h8.ems.audit.dto.AuditSummaryDto;
import com.h8.ems.audit.dto.VerificationResult;
import com.h8.ems.audit.model.AuditLogEntity;
import com.h8.ems.audit.model.ProcessedEventEntity;
import com.h8.ems.audit.repository.AuditLogRepository;
import com.h8.ems.audit.repository.ProcessedEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final String CONSUMER_NAME = "audit-service";

    private final AuditLogRepository auditLogRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final HashChainService hashChainService;
    private final MeterRegistry meterRegistry;
    private final AtomicInteger chainValidGauge = new AtomicInteger(1);

    public AuditService(AuditLogRepository auditLogRepository,
                        ProcessedEventRepository processedEventRepository,
                        HashChainService hashChainService,
                        MeterRegistry meterRegistry) {
        this.auditLogRepository = auditLogRepository;
        this.processedEventRepository = processedEventRepository;
        this.hashChainService = hashChainService;
        this.meterRegistry = meterRegistry;

        Gauge.builder("audit_chain_valid", chainValidGauge, AtomicInteger::get)
                .description("Cryptographic hash chain validity: 1 = valid, 0 = tampered or broken")
                .register(meterRegistry);
    }

    @Transactional
    public synchronized AuditLogDto recordEvent(UUID eventId, String kind, String actor, String payload, Instant at) {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (at == null) {
            at = Instant.now();
        }

        // Idempotency check: deduplicate on eventId
        if (processedEventRepository.existsByConsumerAndEventId(CONSUMER_NAME, eventId)) {
            log.info("Duplicate audit event {} ignored by idempotency dedupe", eventId);
            return auditLogRepository.findByEventId(eventId)
                    .map(this::toDto)
                    .orElse(null);
        }

        String canonicalPayload = hashChainService.canonicalizePayload(payload);
        Instant canonicalAt = hashChainService.canonicalizeAt(at);

        Optional<AuditLogEntity> lastLog = auditLogRepository.findTopByOrderBySeqDesc();
        String prevHash = lastLog.map(AuditLogEntity::getHash).orElse(HashChainService.GENESIS_HASH);

        String hash = hashChainService.calculateHash(prevHash, eventId, kind, canonicalPayload, canonicalAt);

        AuditLogEntity entity = new AuditLogEntity(eventId, kind, actor, canonicalPayload, prevHash, hash, canonicalAt);
        AuditLogEntity saved = auditLogRepository.save(entity);

        processedEventRepository.save(new ProcessedEventEntity(CONSUMER_NAME, eventId));

        Counter.builder("audit_events_recorded_total")
                .tag("kind", kind != null ? kind : "unknown")
                .description("Total audit events recorded into hash chain")
                .register(meterRegistry)
                .increment();

        log.info("Recorded audit log entry seq={} eventId={} kind={} hash={}",
                saved.getSeq(), eventId, kind, hash);

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public VerificationResult verifyIntegrity() {
        List<AuditLogEntity> entries = auditLogRepository.findAllByOrderBySeqAsc();
        VerificationResult result = hashChainService.verifyChain(entries);

        chainValidGauge.set(result.valid() ? 1 : 0);

        if (!result.valid()) {
            log.warn("AUDIT HASH CHAIN TAMPER DETECTED: {}", result.details());
        } else {
            log.info("Audit hash chain integrity verified successfully across {} entries", result.totalEntries());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> query(String kind, String incidentId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 1000);

        if (incidentId != null && !incidentId.isBlank()) {
            return auditLogRepository.findByIncidentIdInPayload(incidentId).stream()
                    .limit(safeLimit)
                    .map(this::toDto)
                    .toList();
        }

        if (kind != null && !kind.isBlank()) {
            return auditLogRepository.findByKindOrderBySeqDesc(kind).stream()
                    .limit(safeLimit)
                    .map(this::toDto)
                    .toList();
        }

        return auditLogRepository.findAllByOrderBySeqDesc(PageRequest.of(0, safeLimit))
                .getContent()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuditSummaryDto getSummary() {
        List<AuditLogEntity> all = auditLogRepository.findAllByOrderBySeqDesc();
        long total = all.size();
        Instant lastAt = all.isEmpty() ? null : all.get(0).getAt();

        Map<String, Long> byKind = all.stream()
                .collect(Collectors.groupingBy(AuditLogEntity::getKind, Collectors.counting()));

        VerificationResult verification = verifyIntegrity();

        return new AuditSummaryDto(total, verification.valid(), lastAt, byKind, verification);
    }

    public AuditLogDto toDto(AuditLogEntity e) {
        return new AuditLogDto(
                e.getSeq(),
                e.getEventId(),
                e.getKind(),
                e.getActor(),
                e.getPayload(),
                e.getPrevHash(),
                e.getHash(),
                e.getAt()
        );
    }
}
