package com.h8.ems.simulator.policy;

import com.h8.ems.common.model.*;
import com.h8.ems.simulator.engine.*;

import java.util.Comparator;
import java.util.UUID;

/** B2: Nearest ALS-aware — sends ALS for ALS-required, nearest otherwise. */
public final class NearestAlsAwarePolicy implements Policy {
    @Override public String name() { return "B2"; }

    @Override
    public UUID selectUnit(IncidentDraw draw, SimState state, TravelModel travelModel) {
        var available = state.availableUnits();
        if (draw.requiresAls()) {
            // Prefer ALS units
            var alsUnit = available.stream()
                    .filter(u -> u.type() == UnitType.ALS)
                    .min(Comparator.comparingDouble(u -> u.position().distanceTo(draw.location())));
            if (alsUnit.isPresent()) return alsUnit.get().id();
        }
        // Fallback: nearest any
        return available.stream()
                .min(Comparator.comparingDouble(u -> u.position().distanceTo(draw.location())))
                .map(UnitSnapshot::id)
                .orElse(null);
    }
}
