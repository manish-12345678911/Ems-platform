package com.h8.ems.simulator.policy;

import com.h8.ems.common.model.*;
import com.h8.ems.common.scoring.*;
import com.h8.ems.simulator.engine.*;

import java.util.UUID;

/**
 * P2: Scorer + hospital destination ranking using DestinationRanker from common.
 */
public final class ScorerWithHospitalPolicy implements Policy {

    private final DispatchScorer scorer;
    private final DestinationRanker ranker;
    private final int capacityTtlMinutes;

    public ScorerWithHospitalPolicy(DispatchScorer scorer, DestinationRanker ranker, int capacityTtlMinutes) {
        this.scorer = scorer;
        this.ranker = ranker;
        this.capacityTtlMinutes = capacityTtlMinutes;
    }

    @Override public String name() { return "P2"; }

    @Override
    public UUID selectUnit(IncidentDraw draw, SimState state, TravelModel travelModel) {
        // Delegate unit selection to P1 logic
        return new ScorerPolicy(scorer).selectUnit(draw, state, travelModel);
    }

    @Override
    public UUID selectHospital(IncidentDraw draw, SimState state,
                                TravelModel travelModel, HospitalModel hospitalModel) {
        IncidentSnapshot incident = new IncidentSnapshot(
                draw.incidentId(), draw.location(), draw.severity(),
                draw.need(), draw.requiresAls(), IncidentStatus.TRIAGED, draw.arrivalTime());

        var ranked = ranker.rank(incident, hospitalModel.allSnapshots(),
                (from, to) -> travelModel.travelSecondsClean(from, to, state.clock()),
                state.clock(), capacityTtlMinutes);

        if (ranked.isEmpty()) {
            return Policy.super.selectHospital(draw, state, travelModel, hospitalModel);
        }
        return ranked.getFirst().hospital().id();
    }
}
