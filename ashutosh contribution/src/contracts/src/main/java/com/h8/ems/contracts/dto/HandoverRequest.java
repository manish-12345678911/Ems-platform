package com.h8.ems.contracts.dto;

import java.util.UUID;

public record HandoverRequest(
        UUID incidentId,
        UUID unitId,
        String notes
) {
}
