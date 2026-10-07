package com.h8.ems.simulator.policy;

import com.h8.ems.common.model.*;
import com.h8.ems.simulator.engine.*;

import java.util.List;
import java.util.UUID;

/**
 * Dispatch policy interface. Each implementation decides which unit to send
 * and (optionally) which hospital to target.
 */
public interface Policy {

    String name();

    /**
     * Select a unit to dispatch for the given incident.
     * @return unitId of the chosen unit, or null if no unit available
     */
    UUID selectUnit(IncidentDraw draw, SimState state, TravelModel travelModel);

    /**
     * Select a destination hospital (for policies P2+).
     * Default: nearest capable hospital.
     */
    default UUID selectHospital(IncidentDraw draw, SimState state,
                                 TravelModel travelModel, HospitalModel hospitalModel) {
        var snapshots = hospitalModel.allSnapshots();
        UUID best = null;
        double bestDist = Double.MAX_VALUE;
        for (var h : snapshots) {
            if (h.supports(draw.need())) {
                double dist = draw.location().distanceTo(h.location());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = h.id();
                }
            }
        }
        // Fallback: any hospital
        if (best == null && !snapshots.isEmpty()) {
            best = snapshots.getFirst().id();
        }
        return best;
    }

    /**
     * Whether this policy runs redeployment after dispatch.
     */
    default boolean usesRedeployment() { return false; }
}
