package com.h8.ems.contracts.dto;

import java.time.Instant;
import java.util.UUID;

public record PreArrivalAlertDto(
        UUID alertId,
        UUID incidentId,
        UUID hospitalId,
        String severity,
        String need,
        int etaSeconds,
        Instant sentAt
) {
}
