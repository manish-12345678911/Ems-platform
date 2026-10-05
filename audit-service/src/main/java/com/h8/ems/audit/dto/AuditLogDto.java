package com.h8.ems.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogDto(
        Long seq,
        UUID eventId,
        String kind,
        String actor,
        String payload,
        String prevHash,
        String hash,
        Instant at
) {}
