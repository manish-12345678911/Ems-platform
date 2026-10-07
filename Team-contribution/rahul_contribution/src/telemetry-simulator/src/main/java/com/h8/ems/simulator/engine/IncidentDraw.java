package com.h8.ems.simulator.engine;

import com.h8.ems.common.model.*;

import java.time.Instant;
import java.util.*;

/**
 * Pre-sampled random draws for a single incident.
 * Keyed by incidentId so the same incident gets identical random values
 * regardless of which policy handles it or when — common random numbers.
 */
public record IncidentDraw(
        UUID incidentId,
        Instant arrivalTime,
        GeoPoint location,
        Severity severity,
        ClinicalNeed need,
        boolean requiresAls,
        double sceneTimeSeconds,
        double travelNoiseFactor,
        double handoverSeconds
) {
}
