package com.h8.ems.contracts.dto;

import java.time.Instant;
import java.util.UUID;

public record RedeploySuggestionDto(
        UUID moveId,
        UUID unitId,
        String callSign,
        String target,
        double coverageGain,
        boolean accepted,
        Instant createdAt
) {
}
