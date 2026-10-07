package com.h8.ems.audit.dto;

import java.time.Instant;
import java.util.Map;

public record AuditSummaryDto(
        long totalEvents,
        boolean chainValid,
        Instant lastEventAt,
        Map<String, Long> eventsByKind,
        VerificationResult verification
) {}
