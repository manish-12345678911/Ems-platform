package com.h8.ems.routing.service;

import com.h8.ems.common.eta.EtaProvider;
import com.h8.ems.common.eta.HaversineEta;
import com.h8.ems.contracts.dto.EtaRequest;
import com.h8.ems.contracts.dto.EtaResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoutingEtaServiceTest {

    @Test
    void fallbackProviderUsedWhenNoPrimary() {
        RoutingEtaService service = new RoutingEtaService(null, new HaversineEta());
        EtaRequest req = new EtaRequest(51.50, -0.12, 51.52, -0.08);

        EtaResponse resp = service.calculateEta(req);

        assertNotNull(resp);
        assertTrue(resp.fallback(), "Should be marked as fallback");
        assertTrue(resp.etaSeconds() > 0, "ETA should be positive");
    }

    @Test
    void primaryProviderUsedWhenAvailable() {
        EtaProvider mockPrimary = (from, to, at) -> 123.45;
        RoutingEtaService service = new RoutingEtaService(mockPrimary, new HaversineEta());
        EtaRequest req = new EtaRequest(51.50, -0.12, 51.52, -0.08);

        EtaResponse resp = service.calculateEta(req);

        assertNotNull(resp);
        assertFalse(resp.fallback(), "Should not be marked as fallback");
        assertEquals(123.45, resp.etaSeconds(), 0.001);
    }

    @Test
    void fallbackMethodInvokedOnFailure() {
        EtaProvider failingPrimary = (from, to, at) -> {
            throw new RuntimeException("Graph error");
        };
        RoutingEtaService service = new RoutingEtaService(failingPrimary, new HaversineEta());
        EtaRequest req = new EtaRequest(51.50, -0.12, 51.52, -0.08);

        EtaResponse resp = service.calculateFallback(req, new RuntimeException("Circuit open"));

        assertNotNull(resp);
        assertTrue(resp.fallback());
        assertTrue(resp.etaSeconds() > 0);
    }
}
