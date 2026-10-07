package com.h8.ems.simulator.policy;

import com.h8.ems.common.model.*;
import com.h8.ems.simulator.engine.*;

import java.util.Comparator;
import java.util.UUID;

/** B1: Nearest available unit (no capability check). */
public final class NearestPolicy implements Policy {
    @Override public String name() { return "B1"; }

    @Override
    public UUID selectUnit(IncidentDraw draw, SimState state, TravelModel travelModel) {
        return state.availableUnits().stream()
                .min(Comparator.comparingDouble(u -> u.position().distanceTo(draw.location())))
                .map(UnitSnapshot::id)
                .orElse(null);
    }
}
