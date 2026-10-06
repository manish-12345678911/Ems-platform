package com.h8.ems.common.eta;

import com.h8.ems.common.model.GeoPoint;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class HaversineEtaTest {

    private final HaversineEta eta = new HaversineEta();

    @Test
    void samePointReturnsZeroEta() {
        var p = new GeoPoint(51.5, -0.1);
        assertEquals(0.0, eta.etaSeconds(p, p, Instant.now()), 0.1);
    }

    @Test
    void etaIsPositiveForDifferentPoints() {
        var a = new GeoPoint(51.5, -0.12);
        var b = new GeoPoint(51.6, -0.10);
        assertTrue(eta.etaSeconds(a, b, Instant.now()) > 0);
    }

    @Test
    void etaIsSymmetric() {
        var a = new GeoPoint(51.5, -0.12);
        var b = new GeoPoint(51.6, -0.10);
        var now = Instant.now();
        assertEquals(eta.etaSeconds(a, b, now), eta.etaSeconds(b, a, now), 0.001);
    }

    @Test
    void etaIncreasesWithDistance() {
        var origin = new GeoPoint(51.5, -0.12);
        var near = new GeoPoint(51.51, -0.12);
        var far = new GeoPoint(51.6, -0.12);
        var now = Instant.now();

        assertTrue(eta.etaSeconds(origin, near, now) < eta.etaSeconds(origin, far, now));
    }
}
