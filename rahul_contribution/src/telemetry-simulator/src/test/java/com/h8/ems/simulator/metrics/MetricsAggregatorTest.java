package com.h8.ems.simulator.metrics;

import com.h8.ems.simulator.engine.SimEngine.IncidentResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MetricsAggregatorTest {

    @Test
    void computesSummaryCorrectly() {
        Instant now = Instant.now();
        List<IncidentResult> results = List.of(
                result(100, now), result(200, now), result(300, now),
                result(400, now), result(500, now)
        );

        var summary = MetricsAggregator.summarize("S1", "B1", 42, results, 480);

        assertEquals(5, summary.totalIncidents());
        assertEquals(5, summary.servedIncidents());
        assertEquals(300, summary.meanResponseSec(), 1.0);
        assertEquals(300, summary.medianResponseSec(), 1.0);
        assertTrue(summary.p90ResponseSec() >= 400);
        assertTrue(summary.p95ResponseSec() >= 400);
        assertEquals(0.8, summary.withinTargetPct(), 0.01); // 500 > 480, so 4/5 = 0.8
    }

    @Test
    void handlesEmptyResults() {
        var summary = MetricsAggregator.summarize("S1", "B1", 42, List.of(), 480);
        assertEquals(0, summary.totalIncidents());
    }

    private IncidentResult result(double responseTime, Instant now) {
        return new IncidentResult(UUID.randomUUID(), "EMERGENCY", "GENERAL",
                now, now, now.plusMillis((long)(responseTime * 1000)),
                null, null, null,
                responseTime, "U1", "ALS", "H1", "B1", true);
    }
}
