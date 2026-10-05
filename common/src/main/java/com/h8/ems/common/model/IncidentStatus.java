package com.h8.ems.common.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Incident status with enforced state machine transitions.
 * See architecture section 5 — incident state diagram.
 */
public enum IncidentStatus {
    RECEIVED,
    TRIAGED,
    DISPATCHED,
    ON_SCENE,
    TRANSPORTING,
    HANDED_OVER,
    CLOSED,
    CANCELLED;

    /**
     * Returns the set of valid next statuses from this status.
     */
    public Set<IncidentStatus> validTransitions() {
        return switch (this) {
            case RECEIVED -> EnumSet.of(TRIAGED, CANCELLED);
            case TRIAGED -> EnumSet.of(DISPATCHED, CANCELLED);
            case DISPATCHED -> EnumSet.of(ON_SCENE, CANCELLED);
            case ON_SCENE -> EnumSet.of(TRANSPORTING);
            case TRANSPORTING -> EnumSet.of(HANDED_OVER);
            case HANDED_OVER -> EnumSet.of(CLOSED);
            case CLOSED, CANCELLED -> EnumSet.noneOf(IncidentStatus.class);
        };
    }

    public boolean canTransitionTo(IncidentStatus target) {
        return validTransitions().contains(target);
    }
}
