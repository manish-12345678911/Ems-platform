package com.h8.ems.common.statemachine;

import com.h8.ems.common.model.UnitStatus;

/**
 * Validates unit status transitions.
 * Used by dispatch-service and simulator to enforce the state machine in architecture section 5.
 */
public final class UnitStateMachine {

    private UnitStateMachine() {
    }

    /**
     * Checks whether a transition from {@code from} to {@code to} is valid.
     *
     * @param from current status
     * @param to   desired status
     * @throws IllegalStateTransitionException if the transition is not allowed
     */
    public static void check(UnitStatus from, UnitStatus to) {
        if (!from.canTransitionTo(to)) {
            throw new IllegalStateTransitionException(
                    "Invalid unit transition: %s -> %s. Allowed: %s"
                            .formatted(from, to, from.validTransitions()));
        }
    }

    /**
     * Returns true if the transition is valid, without throwing.
     */
    public static boolean isValid(UnitStatus from, UnitStatus to) {
        return from.canTransitionTo(to);
    }
}
