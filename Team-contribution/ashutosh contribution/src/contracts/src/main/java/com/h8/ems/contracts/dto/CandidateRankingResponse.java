package com.h8.ems.contracts.dto;

import java.util.UUID;

/**
 * Detailed candidate ranking information with breakdown components for the dispatcher UI.
 */
public record CandidateRankingResponse(
        UUID unitId,
        String callSign,
        String type,
        double score,
        double etaSeconds,
        double distanceKm,
        double etaComponent,
        double capabilityComponent,
        double fatigueComponent,
        double coverageComponent,
        double stalenessComponent
) {
}
