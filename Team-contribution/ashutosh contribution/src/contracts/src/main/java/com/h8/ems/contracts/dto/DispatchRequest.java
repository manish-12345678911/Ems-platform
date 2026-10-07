package com.h8.ems.contracts.dto;

import com.h8.ems.contracts.events.DispatchDecision;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for dispatching or overriding a unit assignment.
 */
public record DispatchRequest(
        UUID incidentId,
        UUID unitId,
        String chosenBy,
        String overrideReason,
        List<DispatchDecision.RankedCandidate> rankedCandidates
) {
    public DispatchRequest(UUID incidentId, UUID unitId, String chosenBy) {
        this(incidentId, unitId, chosenBy, null, List.of());
    }
}
