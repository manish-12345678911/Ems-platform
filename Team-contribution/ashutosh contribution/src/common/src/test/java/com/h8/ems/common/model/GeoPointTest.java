package com.h8.ems.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoPointTest {

    @Test
    void distanceBetweenSamePointIsZero() {
        var p = new GeoPoint(51.5074, -0.1278); // London
        assertEquals(0.0, p.distanceTo(p), 0.001);
    }

    @Test
    void distanceLondonToParisIsAbout340km() {
        var london = new GeoPoint(51.5074, -0.1278);
        var paris = new GeoPoint(48.8566, 2.3522);
        double dist = london.distanceTo(paris);
        assertTrue(dist > 330 && dist < 350,
                "London to Paris should be ~340km, got " + dist);
    }

    @Test
    void distanceIsSymmetric() {
        var a = new GeoPoint(40.7128, -74.0060); // NYC
        var b = new GeoPoint(34.0522, -118.2437); // LA
        assertEquals(a.distanceTo(b), b.distanceTo(a), 0.001);
    }

    @Test
    void distanceToMetersIsThousandTimesKm() {
        var a = new GeoPoint(51.5, -0.1);
        var b = new GeoPoint(51.6, -0.1);
        assertEquals(a.distanceTo(b) * 1000, a.distanceToMeters(b), 0.001);
    }
}
