package com.h8.ems.contracts.dto;

import com.h8.ems.contracts.events.DispatchDecision;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DispatchDtoTest {

    @Test
    void testDispatchRequest() {
        UUID incId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        DispatchRequest req = new DispatchRequest(incId, unitId, "AUTO");

        assertEquals(incId, req.incidentId());
        assertEquals(unitId, req.unitId());
        assertEquals("AUTO", req.chosenBy());
        assertNull(req.overrideReason());
        assertTrue(req.rankedCandidates().isEmpty());

        var candidate = new DispatchDecision.RankedCandidate(unitId, "MEDIC-1", 0.35, 300.0);
        DispatchRequest fullReq = new DispatchRequest(incId, unitId, "DISPATCHER", "Closest available", List.of(candidate));
        assertEquals("DISPATCHER", fullReq.chosenBy());
        assertEquals("Closest available", fullReq.overrideReason());
        assertEquals(1, fullReq.rankedCandidates().size());
    }

    @Test
    void testDispatchResponse() {
        UUID aId = UUID.randomUUID();
        UUID incId = UUID.randomUUID();
        UUID uId = UUID.randomUUID();
        Instant now = Instant.now();

        DispatchResponse resp = new DispatchResponse(aId, incId, uId, "DISPATCHED", now);
        assertEquals(aId, resp.assignmentId());
        assertEquals(incId, resp.incidentId());
        assertEquals(uId, resp.unitId());
        assertEquals("DISPATCHED", resp.status());
        assertEquals(now, resp.dispatchedAt());
    }

    @Test
    void testRejectRequest() {
        UUID incId = UUID.randomUUID();
        UUID uId = UUID.randomUUID();
        RejectRequest req = new RejectRequest(incId, uId, "Mechanical breakdown");

        assertEquals(incId, req.incidentId());
        assertEquals(uId, req.unitId());
        assertEquals("Mechanical breakdown", req.reason());
    }

    @Test
    void testCandidateRankingResponse() {
        UUID uId = UUID.randomUUID();
        CandidateRankingResponse resp = new CandidateRankingResponse(
                uId, "MEDIC-2", "ALS", 0.42, 450.0, 5.2,
                0.20, 0.0, 0.05, 0.12, 0.05
        );

        assertEquals(uId, resp.unitId());
        assertEquals("MEDIC-2", resp.callSign());
        assertEquals("ALS", resp.type());
        assertEquals(0.42, resp.score(), 1e-6);
        assertEquals(450.0, resp.etaSeconds(), 1e-6);
        assertEquals(5.2, resp.distanceKm(), 1e-6);
        assertEquals(0.20, resp.etaComponent(), 1e-6);
    }
}
