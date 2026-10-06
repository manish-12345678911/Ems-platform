package com.h8.ems.common.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Unit status with enforced state machine transitions.
 * See architecture section 5 — unit state diagram.
 */
public enum UnitStatus {
    AVAILABLE,
    DISPATCHED,
    ON_SCENE,
    TRANSPORTING,
    AT_HOSPITAL,
    OFFLINE;

    /**
     * Returns the set of valid next statuses from this status.
     */
    public Set<UnitStatus> validTransitions() {
        return switch (this) {
            case AVAILABLE -> EnumSet.of(DISPATCHED, OFFLINE);
            case DISPATCHED -> EnumSet.of(ON_SCENE, AVAILABLE, OFFLINE);
            case ON_SCENE -> EnumSet.of(TRANSPORTING, AVAILABLE);
            case TRANSPORTING -> EnumSet.of(AT_HOSPITAL);
            case AT_HOSPITAL -> EnumSet.of(AVAILABLE);
            case OFFLINE -> EnumSet.of(AVAILABLE);
        };
    }

    /**
     * Returns true if transitioning from this status to the target is valid.
     */
    public boolean canTransitionTo(UnitStatus target) {
        return validTransitions().contains(target);
    }
}
