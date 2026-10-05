package com.h8.ems.simulator.policy;

import com.h8.ems.common.model.*;
import com.h8.ems.common.scoring.*;
import com.h8.ems.simulator.engine.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * P1: DispatchScorer-based policy — uses the full multi-factor scorer from common.
 * Imports DispatchScorer (never copies it, per rule #2).
 */
public final class ScorerPolicy implements Policy {

    private final DispatchScorer scorer;

    public ScorerPolicy(DispatchScorer scorer) {
        this.scorer = scorer;
    }

    @Override public String name() { return "P1"; }

    @Override
    public UUID selectUnit(IncidentDraw draw, SimState state, TravelModel travelModel) {
        List<UnitSnapshot> available = state.availableUnits();
        if (available.isEmpty()) return null;

        IncidentSnapshot incident = new IncidentSnapshot(
                draw.incidentId(), draw.location(), draw.severity(),
                draw.need(), draw.requiresAls(), IncidentStatus.TRIAGED, draw.arrivalTime());

        return available.stream()
                .min(Comparator.comparingDouble(u -> {
                    double eta = travelModel.travelSecondsClean(u.position(), draw.location(), state.clock());
                    return scorer.score(u, incident, eta, state.clock(), available);
                }))
                .map(UnitSnapshot::id)
                .orElse(null);
    }
}
