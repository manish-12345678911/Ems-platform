package com.h8.ems.simulator.policy;

import com.h8.ems.common.scoring.*;
import com.h8.ems.simulator.engine.*;

import java.util.UUID;

/**
 * P3: Scorer + hospital ranking + redeployment (with optional ablation flags for E5).
 * Uses RedeploymentPlanner from common (never copies it).
 */
public final class FullPolicy implements Policy {

    public record AblationFlags(
            boolean disableRedeployment,
            boolean disableHospitalRanking,
            boolean zeroCoverageWeight,
            boolean zeroFatigueWeight
    ) {
        public static AblationFlags none() {
            return new AblationFlags(false, false, false, false);
        }
    }

    private final DispatchScorer scorer;
    private final DestinationRanker ranker;
    private final int capacityTtlMinutes;
    private final AblationFlags ablations;

    public FullPolicy(DispatchScorer scorer, DestinationRanker ranker, int capacityTtlMinutes) {
        this(scorer, ranker, capacityTtlMinutes, AblationFlags.none());
    }

    public FullPolicy(DispatchScorer scorer, DestinationRanker ranker, int capacityTtlMinutes, AblationFlags ablations) {
        this.scorer = scorer;
        this.ranker = ranker;
        this.capacityTtlMinutes = capacityTtlMinutes;
        this.ablations = ablations != null ? ablations : AblationFlags.none();
    }

    public AblationFlags ablations() {
        return ablations;
    }

    @Override
    public String name() {
        if (ablations.disableRedeployment() && ablations.disableHospitalRanking()) {
            return "P3-minimal";
        } else if (ablations.disableRedeployment()) {
            return "P3-no-redeploy";
        } else if (ablations.disableHospitalRanking()) {
            return "P3-no-hosp";
        } else if (ablations.zeroCoverageWeight()) {
            return "P3-no-coverage";
        } else if (ablations.zeroFatigueWeight()) {
            return "P3-no-fatigue";
        }
        return "P3";
    }

    @Override
    public UUID selectUnit(IncidentDraw draw, SimState state, TravelModel travelModel) {
        return new ScorerPolicy(scorer).selectUnit(draw, state, travelModel);
    }

    @Override
    public UUID selectHospital(IncidentDraw draw, SimState state,
                                TravelModel travelModel, HospitalModel hospitalModel) {
        if (ablations.disableHospitalRanking()) {
            return Policy.super.selectHospital(draw, state, travelModel, hospitalModel);
        }
        return new ScorerWithHospitalPolicy(scorer, ranker, capacityTtlMinutes)
                .selectHospital(draw, state, travelModel, hospitalModel);
    }

    @Override
    public boolean usesRedeployment() {
        return !ablations.disableRedeployment();
    }
}
