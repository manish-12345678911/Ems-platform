package com.h8.ems.contracts.dto;

import java.time.Instant;
import java.util.UUID;

public record HospitalCapacityResponse(
        UUID hospitalId,
        String name,
        int edBedsFree,
        int icuBedsFree,
        int ventilatorsFree,
        Instant updatedAt,
        boolean isStale
) {
}
