package com.h8.ems.common.model;

/**
 * Severity levels for incidents.
 * CRITICAL incidents get the highest priority and scale factor.
 */
public enum Severity {
    CRITICAL(0.25),
    EMERGENCY(0.50),
    URGENT(0.75),
    LOW(1.0);

    private final double scaleFactor;

    Severity(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    /**
     * Scale factor applied to ETA in dispatch scoring.
     * Lower severity = lower factor = higher effective priority.
     */
    public double scaleFactor() {
        return scaleFactor;
    }
}
