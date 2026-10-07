package com.h8.ems.routing.service;

import com.h8.ems.common.eta.HaversineEta;
import com.h8.ems.common.model.GeoPoint;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class GraphHopperEtaTest {

    @Test
    void fallbackUsedWhenGraphNotLoaded() {
        GraphHopperEta ghEta = new GraphHopperEta("non_existent.osm.pbf", "non_existent_cache", new HaversineEta());
        assertFalse(ghEta.isLoaded(), "Should not be loaded with dummy paths");

        GeoPoint from = new GeoPoint(51.50, -0.12);
        GeoPoint to = new GeoPoint(51.55, -0.10);
        double eta = ghEta.etaSeconds(from, to, Instant.now());

        assertTrue(eta > 0, "Fallback ETA should be positive");
    }

    @Test
    void timeOfDayFactorCalculatesCorrectly() {
        GraphHopperEta ghEta = new GraphHopperEta();

        // Morning peak: 08:30 UTC
        Instant morningPeak = ZonedDateTime.of(2026, 10, 3, 8, 30, 0, 0, ZoneOffset.UTC).toInstant();
        assertEquals(1.30, ghEta.timeOfDayFactor(morningPeak), 0.001);

        // Evening peak: 17:30 UTC
        Instant eveningPeak = ZonedDateTime.of(2026, 10, 3, 17, 30, 0, 0, ZoneOffset.UTC).toInstant();
        assertEquals(1.30, ghEta.timeOfDayFactor(eveningPeak), 0.001);

        // Night free-flow: 02:00 UTC
        Instant night = ZonedDateTime.of(2026, 10, 3, 2, 0, 0, 0, ZoneOffset.UTC).toInstant();
        assertEquals(0.90, ghEta.timeOfDayFactor(night), 0.001);

        // Off-peak: 12:00 UTC
        Instant offPeak = ZonedDateTime.of(2026, 10, 3, 12, 0, 0, 0, ZoneOffset.UTC).toInstant();
        assertEquals(1.00, ghEta.timeOfDayFactor(offPeak), 0.001);

        // Null time defaults to 1.00
        assertEquals(1.00, ghEta.timeOfDayFactor(null), 0.001);
    }
}
