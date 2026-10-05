package com.h8.ems.simulator.engine;

import com.h8.ems.common.eta.EtaProvider;
import com.h8.ems.common.eta.HaversineEta;
import com.h8.ems.common.model.GeoPoint;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TravelModelTest {

    private static final Instant NOW = Instant.parse("2025-01-01T10:00:00Z");

    @Test
    void cleanEtaPassesThrough() {
        EtaProvider eta = new HaversineEta();
        TravelModel model = new TravelModel(eta);

        GeoPoint a = new GeoPoint(51.50, -0.12);
        GeoPoint b = new GeoPoint(51.52, -0.08);

        double clean = model.travelSecondsClean(a, b, NOW);
        double expected = eta.etaSeconds(a, b, NOW);

        assertEquals(expected, clean, 0.001, "Clean ETA should pass through without modification");
    }

    @Test
    void noiseMultipliesBaseEta() {
        EtaProvider eta = new HaversineEta();
        TravelModel model = new TravelModel(eta);

        GeoPoint a = new GeoPoint(51.50, -0.12);
        GeoPoint b = new GeoPoint(51.52, -0.08);
        double noiseFactor = 1.5;

        double noisy = model.travelSeconds(a, b, NOW, noiseFactor);
        double base = eta.etaSeconds(a, b, NOW);

        assertEquals(base * noiseFactor, noisy, 0.001, "Noisy ETA = base * noiseFactor");
    }

    @Test
    void noiseFactorOfOneEqualsClean() {
        TravelModel model = new TravelModel(new HaversineEta());

        GeoPoint a = new GeoPoint(51.50, -0.12);
        GeoPoint b = new GeoPoint(51.52, -0.08);

        double clean = model.travelSecondsClean(a, b, NOW);
        double noisy = model.travelSeconds(a, b, NOW, 1.0);

        assertEquals(clean, noisy, 0.001, "Noise factor 1.0 should equal clean ETA");
    }

    @Test
    void zeroDistanceReturnsZero() {
        TravelModel model = new TravelModel(new HaversineEta());
        GeoPoint p = new GeoPoint(51.50, -0.12);

        double clean = model.travelSecondsClean(p, p, NOW);
        double noisy = model.travelSeconds(p, p, NOW, 1.5);

        assertEquals(0.0, clean, 0.001, "Zero distance → zero clean ETA");
        assertEquals(0.0, noisy, 0.001, "Zero distance → zero noisy ETA");
    }
}
