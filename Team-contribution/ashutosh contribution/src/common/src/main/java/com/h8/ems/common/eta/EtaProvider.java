package com.h8.ems.common.eta;

import com.h8.ems.common.model.GeoPoint;

import java.time.Instant;

/**
 * Interface for ETA calculation.
 * Implementations: HaversineEta (fallback), GraphHopperEta (routing-service).
 * The simulator uses HaversineEta + noise; the live system uses GraphHopper with fallback.
 */
public interface EtaProvider {

    /**
     * Estimates travel time in seconds from origin to destination.
     *
     * @param from origin point
     * @param to   destination point
     * @param at   the time of travel (for time-of-day factors)
     * @return estimated travel time in seconds
     */
    double etaSeconds(GeoPoint from, GeoPoint to, Instant at);
}
