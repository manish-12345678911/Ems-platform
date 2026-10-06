package com.h8.ems.common.scoring;

import com.h8.ems.common.model.*;

import java.time.Instant;
import java.util.List;

/**
 * Scores a candidate unit for dispatch to an incident.
 * Lower score = better candidate.
 *
 * Components:
 * - ETA (travel time)
 * - Capability match (ALS requirement)
 * - Fatigue (hours on shift)
 * - Coverage impact (removing this unit from available pool)
 * - Staleness penalty (old position data)
 *
 * Per architecture section 10 and correction #6.
 */
public final class DispatchScorer {

    private final ScorerParams params;
    private final CoverageModel coverageModel;

    public DispatchScorer(ScorerParams params, CoverageModel coverageModel) {
        this.params = params;
        this.coverageModel = coverageModel;
    }

    public DispatchScorer(CoverageModel coverageModel) {
        this(ScorerParams.defaults(), coverageModel);
    }

    /**
     * Scores a candidate unit for a given incident. Lower is better.
     *
     * @param unit           the candidate unit snapshot
     * @param incident       the incident to dispatch for
     * @param etaSeconds     pre-computed ETA in seconds
     * @param now            current time
     * @param availableUnits all currently available units (for coverage calc)
     * @return composite score (lower = better match)
     */
    public double score(UnitSnapshot unit, IncidentSnapshot incident,
                        double etaSeconds, Instant now,
                        List<UnitSnapshot> availableUnits) {
        double etaScore = normalizeEta(etaSeconds) * incident.severity().scaleFactor();
        double capScore = capabilityScore(unit, incident);
        double fatigueScore = fatigueScore(unit, now);
        double coverScore = coverageModel.lossIfRemoved(unit, availableUnits);
        double staleScore = stalenessScore(unit, now);

        return params.etaWeight() * etaScore
                + params.capabilityWeight() * capScore
                + params.fatigueWeight() * fatigueScore
                + params.coverageWeight() * coverScore
                + params.stalePenaltyWeight() * staleScore;
    }

    /**
     * Returns a breakdown of score components for transparency in the dispatcher UI.
     */
    public ScoreBreakdown breakdown(UnitSnapshot unit, IncidentSnapshot incident,
                                     double etaSeconds, Instant now,
                                     List<UnitSnapshot> availableUnits) {
        double etaScore = normalizeEta(etaSeconds) * incident.severity().scaleFactor();
        double capScore = capabilityScore(unit, incident);
        double fatigueScore = fatigueScore(unit, now);
        double coverScore = coverageModel.lossIfRemoved(unit, availableUnits);
        double staleScore = stalenessScore(unit, now);
        double total = params.etaWeight() * etaScore
                + params.capabilityWeight() * capScore
                + params.fatigueWeight() * fatigueScore
                + params.coverageWeight() * coverScore
                + params.stalePenaltyWeight() * staleScore;

        return new ScoreBreakdown(total, etaSeconds, etaScore, capScore,
                fatigueScore, coverScore, staleScore);
    }

    /** Normalize ETA to [0, 1] range. 30 min is the reference max. */
    private double normalizeEta(double etaSeconds) {
        return Math.min(etaSeconds / 1800.0, 1.0);
    }

    /**
     * Capability mismatch penalty.
     * If incident requires ALS and unit is BLS: maximum penalty.
     * Per correction #6: ALS-needed CRITICAL cases must not be beaten by BLS units.
     */
    private double capabilityScore(UnitSnapshot unit, IncidentSnapshot incident) {
        if (incident.requiresAls() && unit.type() == UnitType.BLS) {
            return 1.0; // full penalty — BLS cannot handle ALS-required incident
        }
        return 0.0; // no penalty
    }

    /** Fatigue increases linearly after the threshold. */
    private double fatigueScore(UnitSnapshot unit, Instant now) {
        double hours = unit.hoursOnShift(now);
        if (hours <= params.fatigueThresholdHours()) return 0.0;
        return Math.min((hours - params.fatigueThresholdHours()) / 4.0, 1.0);
    }

    /** Stale position data gets penalised. */
    private double stalenessScore(UnitSnapshot unit, Instant now) {
        return unit.isPositionStale(now, (long) params.staleTtlSeconds()) ? 1.0 : 0.0;
    }

    /**
     * Score breakdown for dispatcher UI transparency.
     */
    public record ScoreBreakdown(
            double totalScore,
            double etaSeconds,
            double etaComponent,
            double capabilityComponent,
            double fatigueComponent,
            double coverageComponent,
            double stalenessComponent
    ) {
    }
}
