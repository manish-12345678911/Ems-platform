package com.h8.ems.common.statemachine;

/**
 * Thrown when an invalid state transition is attempted.
 * Services catch this and return HTTP 409 Conflict.
 */
public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
