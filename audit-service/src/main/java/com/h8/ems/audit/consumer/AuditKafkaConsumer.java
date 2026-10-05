package com.h8.ems.audit.consumer;

import com.h8.ems.audit.service.AuditService;
import com.h8.ems.contracts.events.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuditKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditKafkaConsumer.class);

    private final AuditService auditService;

    public AuditKafkaConsumer(AuditService auditService) {
        this.auditService = auditService;
    }

    @KafkaListener(topics = "audit.events", groupId = "audit-service")
    public void onAuditEvent(AuditEvent event) {
        if (event == null) {
            log.warn("Received null AuditEvent");
            return;
        }

        log.debug("Consumed AuditEvent eventId={} kind={} actor={}",
                event.eventId(), event.kind(), event.actor());

        auditService.recordEvent(
                event.eventId(),
                event.kind(),
                event.actor(),
                event.payload(),
                event.at()
        );
    }
}
