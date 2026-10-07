package com.h8.ems.simulator.engine;

import com.h8.ems.common.eta.EtaProvider;
import com.h8.ems.common.model.GeoPoint;

import java.time.Instant;

/**
 * Travel model wrapping EtaProvider with log-normal noise for realistic variation.
 * The noise factor is pre-sampled per incident (from IncidentDraw.travelNoiseFactor)
 * to maintain common random numbers across policies.
 */
public final class TravelModel {

    private final EtaProvider etaProvider;

    public TravelModel(EtaProvider etaProvider) {
        this.etaProvider = etaProvider;
    }

    /**
     * Compute travel time with pre-sampled noise.
     *
     * @param from            origin
     * @param to              destination
     * @param at              time of travel
     * @param noiseFactor     pre-sampled log-normal factor from IncidentDraw
     * @return travel time in seconds with noise applied
     */
    public double travelSeconds(GeoPoint from, GeoPoint to, Instant at, double noiseFactor) {
        double baseEta = etaProvider.etaSeconds(from, to, at);
        return baseEta * noiseFactor;
    }

    /**
     * Compute travel time without noise (for deterministic ETA display).
     */
    public double travelSecondsClean(GeoPoint from, GeoPoint to, Instant at) {
        return etaProvider.etaSeconds(from, to, at);
    }
}
