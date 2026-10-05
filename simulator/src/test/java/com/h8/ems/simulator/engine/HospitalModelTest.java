package com.h8.ems.simulator.engine;

import com.h8.ems.common.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HospitalModelTest {

    private HospitalModel model;
    private UUID hospitalId;

    @BeforeEach
    void setUp() {
        model = new HospitalModel();
        hospitalId = UUID.randomUUID();
        model.initHospital(hospitalId, "Test Hospital", new GeoPoint(51.50, -0.12),
                Set.of(ClinicalNeed.TRAUMA, ClinicalNeed.CARDIAC, ClinicalNeed.GENERAL),
                20, 5, 3);
    }

    @Test
    void snapshotReflectsInitialState() {
        HospitalSnapshot snap = model.snapshot(hospitalId);

        assertNotNull(snap);
        assertEquals("Test Hospital", snap.name());
        assertEquals(20, snap.edBedsFree());
        assertEquals(5, snap.icuBedsFree());
        assertEquals(3, snap.ventilatorsFree());
        assertTrue(snap.supports(ClinicalNeed.TRAUMA));
        assertTrue(snap.supports(ClinicalNeed.CARDIAC));
        assertFalse(snap.supports(ClinicalNeed.STROKE));
    }

    @Test
    void admitDecrementsEdBeds() {
        model.admitPatient(hospitalId);
        assertEquals(19, model.snapshot(hospitalId).edBedsFree());
    }

    @Test
    void releaseIncrementsEdBeds() {
        model.admitPatient(hospitalId);
        model.releasePatient(hospitalId);
        assertEquals(20, model.snapshot(hospitalId).edBedsFree());
    }

    @Test
    void releaseDoesNotExceedCapacity() {
        // Release without prior admit should not exceed initial beds
        model.releasePatient(hospitalId);
        assertEquals(20, model.snapshot(hospitalId).edBedsFree());
    }

    @Test
    void admitDoesNotGoBelowZero() {
        for (int i = 0; i < 25; i++) {
            model.admitPatient(hospitalId);
        }
        assertTrue(model.snapshot(hospitalId).edBedsFree() >= 0);
    }

    @Test
    void updateCapacityTimestamp() {
        Instant now = Instant.now();
        model.updateCapacityTimestamp(hospitalId, now);
        assertEquals(now, model.snapshot(hospitalId).capacityUpdatedAt());
    }

    @Test
    void allSnapshotsReturnsAll() {
        UUID h2 = UUID.randomUUID();
        model.initHospital(h2, "Hospital 2", new GeoPoint(51.52, -0.10),
                Set.of(ClinicalNeed.GENERAL), 15, 3, 2);

        List<HospitalSnapshot> all = model.allSnapshots();
        assertEquals(2, all.size());
    }

    @Test
    void allIdsReturnsAll() {
        UUID h2 = UUID.randomUUID();
        model.initHospital(h2, "Hospital 2", new GeoPoint(51.52, -0.10),
                Set.of(ClinicalNeed.GENERAL), 15, 3, 2);

        List<UUID> ids = model.allIds();
        assertEquals(2, ids.size());
        assertTrue(ids.contains(hospitalId));
        assertTrue(ids.contains(h2));
    }

    @Test
    void snapshotForUnknownIdReturnsNull() {
        assertNull(model.snapshot(UUID.randomUUID()));
    }
}
