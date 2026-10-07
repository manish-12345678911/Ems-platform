package com.h8.ems.contracts.dto;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NearbyUnitResponseTest {

    @Test
    void createsNearbyUnitResponseCorrectly() {
        UUID id = UUID.randomUUID();
        NearbyUnitResponse resp = new NearbyUnitResponse(id, 2.5, 51.50, -0.12, 1700000000000L);

        assertEquals(id, resp.unitId());
        assertEquals(2.5, resp.distanceKm());
        assertEquals(51.50, resp.lat());
        assertEquals(-0.12, resp.lon());
        assertEquals(1700000000000L, resp.lastSeenEpochMs());
    }
}
