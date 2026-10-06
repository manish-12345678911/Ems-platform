package com.h8.ems.common.model;

/**
 * Immutable geographic point with latitude and longitude.
 * Uses the Haversine formula for distance calculations.
 */
public record GeoPoint(double lat, double lon) {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculates the great-circle distance to another point using the Haversine formula.
     *
     * @param other the destination point
     * @return distance in kilometers
     */
    public double distanceTo(GeoPoint other) {
        double dLat = Math.toRadians(other.lat - this.lat);
        double dLon = Math.toRadians(other.lon - this.lon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(this.lat)) * Math.cos(Math.toRadians(other.lat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Returns the distance in meters.
     */
    public double distanceToMeters(GeoPoint other) {
        return distanceTo(other) * 1000.0;
    }
}
