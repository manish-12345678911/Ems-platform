package com.h8.ems.common.scoring;

import com.h8.ems.common.model.GeoPoint;
import com.h8.ems.common.model.UnitSnapshot;
import com.h8.ems.common.model.UnitStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Greedy redeployment planner.
 * Moves idle (AVAILABLE) units to standby points to maximize coverage.
 * Per architecture section 10 and correction #2 (operates on UnitSnapshot copies, not JPA entities).
 */
public final class RedeploymentPlanner {

    private final CoverageModel coverageModel;

    public RedeploymentPlanner(CoverageModel coverageModel) {
        this.coverageModel = coverageModel;
    }

    /**
     * Plans redeployment moves for idle units.
     *
     * @param idleUnits     units with status AVAILABLE
     * @param standbyPoints candidate standby locations (stations, strategic points)
     * @param limits        constraints on the planner
     * @return ordered list of suggested moves
     */
    public List<RedeployMove> plan(List<UnitSnapshot> idleUnits,
                                    List<GeoPoint> standbyPoints,
                                    PlannerLimits limits) {
        List<RedeployMove> moves = new ArrayList<>();
        // Work on snapshot copies per correction #2
        List<UnitSnapshot> workingSet = new ArrayList<>(idleUnits);
        double currentCoverage = coverageModel.coverage(workingSet);

        for (int i = 0; i < limits.maxMoves(); i++) {
            RedeployMove bestMove = null;
            double bestGain = 0.0;

            for (UnitSnapshot unit : workingSet) {
                if (unit.status() != UnitStatus.AVAILABLE) continue;

                for (GeoPoint sp : standbyPoints) {
                    double newCoverage = coverageModel.coverageIfMoved(workingSet, unit, sp);
                    double gain = newCoverage - currentCoverage;

                    if (gain > limits.minGain() && gain > bestGain) {
                        bestGain = gain;
                        bestMove = new RedeployMove(unit.id(), unit.callSign(),
                                unit.position(), sp, gain);
                    }
                }
            }

            if (bestMove == null) break; // no beneficial move found

            moves.add(bestMove);

            // Update working set with the move applied (snapshot copy)
            final RedeployMove finalMove = bestMove;
            workingSet = workingSet.stream()
                    .map(u -> u.id().equals(finalMove.unitId())
                            ? new UnitSnapshot(u.id(), u.callSign(), u.type(), u.status(),
                            finalMove.target(), u.positionAt(), u.shiftStart(),
                            u.homeStationId(), u.homeStationLocation())
                            : u)
                    .toList();
            workingSet = new ArrayList<>(workingSet); // mutable copy for next iteration
            currentCoverage += bestGain;
        }

        return Collections.unmodifiableList(moves);
    }

    /**
     * A suggested redeployment move.
     */
    public record RedeployMove(
            UUID unitId,
            String callSign,
            GeoPoint currentPosition,
            GeoPoint target,
            double coverageGain
    ) {
    }

    /**
     * Planner limits — loaded from config.
     */
    public record PlannerLimits(
            int maxMoves,
            double minGain,
            int cooldownMinutes
    ) {
        public static PlannerLimits defaults() {
            return new PlannerLimits(3, 0.01, 10);
        }
    }
}
