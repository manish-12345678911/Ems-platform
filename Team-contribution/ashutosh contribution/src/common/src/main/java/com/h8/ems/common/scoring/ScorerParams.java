package com.h8.ems.common.scoring;

/**
 * Tunable parameters for dispatch scoring.
 * Loaded from YAML config so experiments (E5 sensitivity) just swap config files.
 */
public record ScorerParams(
        double etaWeight,
        double capabilityWeight,
        double fatigueWeight,
        double coverageWeight,
        double stalePenaltyWeight,
        double fatigueThresholdHours,
        double staleTtlSeconds,
        double coverageRadiusKm
) {
    /**
     * Default parameters for dispatch scoring.
     */
    public static ScorerParams defaults() {
        return new ScorerParams(
                0.40,   // etaWeight
                0.25,   // capabilityWeight
                0.10,   // fatigueWeight
                0.15,   // coverageWeight
                0.10,   // stalePenaltyWeight
                10.0,   // fatigueThresholdHours
                90.0,   // staleTtlSeconds
                8.0     // coverageRadiusKm
        );
    }
}
