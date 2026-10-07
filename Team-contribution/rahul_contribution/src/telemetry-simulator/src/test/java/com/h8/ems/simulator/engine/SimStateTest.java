package com.h8.ems.simulator.engine;

import com.h8.ems.common.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SimStateTest {

    private static final Instant SIM_START = Instant.parse("2025-01-01T08:00:00Z");
    private SimState state;
    private UUID unitId;
    private UUID stationId;
    private GeoPoint homeStation;

    @BeforeEach
    void setUp() {
        state = new SimState(SIM_START);
        unitId = UUID.randomUUID();
        stationId = UUID.randomUUID();
        homeStation = new GeoPoint(51.50, -0.12);
        state.addUnit(unitId, "U1", UnitType.ALS, homeStation, stationId);
    }

    @Test
    void addUnitMakesItAvailable() {
        List<UnitSnapshot> available = state.availableUnits();
        assertEquals(1, available.size());
        assertEquals(unitId, available.getFirst().id());
        assertEquals(UnitStatus.AVAILABLE, available.getFirst().status());
        assertEquals("U1", available.getFirst().callSign());
        assertEquals(UnitType.ALS, available.getFirst().type());
    }

    @Test
    void dispatchRemovesFromAvailable() {
        state.dispatchUnit(unitId, new GeoPoint(51.52, -0.10));
        assertTrue(state.availableUnits().isEmpty());
        assertEquals(UnitStatus.DISPATCHED, state.unitSnapshot(unitId).status());
    }

    @Test
    void arrivedSceneSetsOnScene() {
        state.dispatchUnit(unitId, new GeoPoint(51.52, -0.10));
        state.unitArrivedScene(unitId);
        assertEquals(UnitStatus.ON_SCENE, state.unitSnapshot(unitId).status());
    }

    @Test
    void transportingSetsTransporting() {
        state.dispatchUnit(unitId, new GeoPoint(51.52, -0.10));
        state.unitArrivedScene(unitId);
        state.unitTransporting(unitId);
        assertEquals(UnitStatus.TRANSPORTING, state.unitSnapshot(unitId).status());
    }

    @Test
    void atHospitalSetsStatusAndPosition() {
        GeoPoint hospLoc = new GeoPoint(51.52, -0.08);
        state.dispatchUnit(unitId, new GeoPoint(51.52, -0.10));
        state.unitArrivedScene(unitId);
        state.unitTransporting(unitId);
        state.unitAtHospital(unitId, hospLoc);

        UnitSnapshot snap = state.unitSnapshot(unitId);
        assertEquals(UnitStatus.AT_HOSPITAL, snap.status());
        assertEquals(hospLoc, snap.position());
    }

    @Test
    void unitAvailableRestoresAvailability() {
        state.dispatchUnit(unitId, new GeoPoint(51.52, -0.10));
        state.unitArrivedScene(unitId);
        state.unitAvailable(unitId);

        assertEquals(1, state.availableUnits().size());
        assertEquals(UnitStatus.AVAILABLE, state.unitSnapshot(unitId).status());
    }

    @Test
    void moveUnitUpdatesPosition() {
        GeoPoint newPos = new GeoPoint(51.53, -0.15);
        state.moveUnit(unitId, newPos);
        assertEquals(newPos, state.unitSnapshot(unitId).position());
    }

    @Test
    void clockAdvances() {
        assertEquals(SIM_START, state.clock());
        Instant later = SIM_START.plusSeconds(3600);
        state.advanceClock(later);
        assertEquals(later, state.clock());
    }

    @Test
    void allUnitSnapshotsReturnsAll() {
        UUID u2 = UUID.randomUUID();
        state.addUnit(u2, "U2", UnitType.BLS, new GeoPoint(51.48, -0.15), stationId);
        state.dispatchUnit(unitId, new GeoPoint(51.52, -0.10));

        assertEquals(2, state.allUnitSnapshots().size());
        assertEquals(1, state.availableUnits().size());
    }

    @Test
    void unitSnapshotForUnknownIdReturnsNull() {
        assertNull(state.unitSnapshot(UUID.randomUUID()));
    }

    @Test
    void registerAndGetDraw() {
        IncidentDraw draw = new IncidentDraw(UUID.randomUUID(), SIM_START,
                new GeoPoint(51.50, -0.12), Severity.EMERGENCY, ClinicalNeed.GENERAL,
                false, 900, 1.0, 1200);
        state.registerDraw(draw);
        assertEquals(draw, state.getDraw(draw.incidentId()));
    }

    @Test
    void activeIncidentLifecycle() {
        IncidentDraw draw = new IncidentDraw(UUID.randomUUID(), SIM_START,
                new GeoPoint(51.50, -0.12), Severity.EMERGENCY, ClinicalNeed.GENERAL,
                false, 900, 1.0, 1200);
        UUID incId = draw.incidentId();

        state.addActiveIncident(incId, draw);
        assertNotNull(state.getActiveIncident(incId));
        assertEquals(incId, state.getActiveIncident(incId).incidentId);

        state.removeActiveIncident(incId);
        assertNull(state.getActiveIncident(incId));
    }
}
