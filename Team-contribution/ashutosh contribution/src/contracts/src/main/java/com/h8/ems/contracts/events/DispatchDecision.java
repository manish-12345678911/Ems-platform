package com.h8.ems.contracts.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Kafka event for dispatch decisions.
 * Topic: dispatch.decisions, Key: incidentId
 */
public record DispatchDecision(
        UUID eventId,
        UUID incidentId,
        UUID unitId,
        String chosenBy,
        List<RankedCandidate> ranked,
        Instant at,
        int schemaVersion
) {
    public record RankedCandidate(UUID unitId, String callSign, double score, double etaSeconds) {
    }

    public DispatchDecision(UUID eventId, UUID incidentId, UUID unitId,
                             String chosenBy, List<RankedCandidate> ranked, Instant at) {
        this(eventId, incidentId, unitId, chosenBy, ranked, at, 1);
    }
}
