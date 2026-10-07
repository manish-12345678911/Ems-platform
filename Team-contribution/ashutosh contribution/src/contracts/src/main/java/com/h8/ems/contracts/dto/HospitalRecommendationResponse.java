package com.h8.ems.contracts.dto;

import java.util.UUID;

public record HospitalRecommendationResponse(
        UUID hospitalId,
        String name,
        double score,
        double transportEtaSeconds,
        double estimatedWaitMinutes,
        boolean capacityStale
) {
}
