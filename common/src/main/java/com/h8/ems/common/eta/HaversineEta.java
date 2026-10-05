package com.h8.ems.common.eta;

import com.h8.ems.common.model.GeoPoint;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Haversine-based ETA estimation.
 * Assumes an average speed and applies a time-of-day factor.
 * Used as the fallback when GraphHopper is unavailable (per correction #10).
 */
public final class HaversineEta implements EtaProvider {

    /** Average ambulance speed in km/h under normal conditions. */
    private static final double BASE_SPEED_KMH = 50.0;

    @Override
    public double etaSeconds(GeoPoint from, GeoPoint to, Instant at) {
        double distanceKm = from.distanceTo(to);
        double speedKmh = BASE_SPEED_KMH * timeOfDayFactor(at);
        double hours = distanceKm / speedKmh;
        return hours * 3600.0;
    }

    /**
     * Time-of-day speed factor.
     * Rush hours (7-9, 16-18) slow down; nighttime (22-6) speeds up.
     */
    private double timeOfDayFactor(Instant at) {
        if (at == null) return 1.0;
        int hour = at.atOffset(ZoneOffset.UTC).getHour();
        if ((hour >= 7 && hour <= 9) || (hour >= 16 && hour <= 18)) {
            return 0.7; // rush hour
        }
        if (hour >= 22 || hour <= 6) {
            return 1.3; // nighttime — faster
        }
        return 1.0; // daytime normal
    }
}
