package com.h8.ems.dispatch.model;

import com.h8.ems.common.model.UnitSnapshot;
import com.h8.ems.common.model.UnitStatus;
import com.h8.ems.common.model.UnitType;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AmbulanceUnitEntityTest {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Test
    void defaultStatusIsAvailable() {
        AmbulanceUnitEntity unit = new AmbulanceUnitEntity();
        assertEquals(UnitStatus.AVAILABLE, unit.getStatus());
    }

    @Test
    void toSnapshotMapsFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        Point pos = GF.createPoint(new Coordinate(-0.1278, 51.5074)); // London
        Instant now = Instant.now();
        Instant shiftStart = now.minusSeconds(3600);

        AmbulanceUnitEntity unit = new AmbulanceUnitEntity(
                id, "MEDIC-1", UnitType.ALS, UnitStatus.AVAILABLE,
                pos, now, shiftStart, null
        );

        UnitSnapshot snapshot = unit.toSnapshot();
        assertEquals(id, snapshot.id());
        assertEquals("MEDIC-1", snapshot.callSign());
        assertEquals(UnitType.ALS, snapshot.type());
        assertEquals(UnitStatus.AVAILABLE, snapshot.status());
        assertNotNull(snapshot.position());
        assertEquals(51.5074, snapshot.position().lat(), 0.001);
        assertEquals(-0.1278, snapshot.position().lon(), 0.001);
        assertEquals(now, snapshot.positionAt());
        assertEquals(shiftStart, snapshot.shiftStart());
    }

    @Test
    void toSnapshotHandlesNullPosition() {
        AmbulanceUnitEntity unit = new AmbulanceUnitEntity(
                UUID.randomUUID(), "BLS-1", UnitType.BLS, UnitStatus.AVAILABLE,
                null, null, null, null
        );
        UnitSnapshot snapshot = unit.toSnapshot();
        assertNull(snapshot.position());
        assertNull(snapshot.positionAt());
    }

    @Test
    void jsonLatLonExtractionFromPoint() {
        Point pos = GF.createPoint(new Coordinate(-73.9857, 40.7484)); // NYC
        AmbulanceUnitEntity unit = new AmbulanceUnitEntity();
        unit.setPosition(pos);

        assertEquals(40.7484, unit.getLat(), 0.001);
        assertEquals(-73.9857, unit.getLon(), 0.001);
    }

    @Test
    void nullPositionGivesNullLatLon() {
        AmbulanceUnitEntity unit = new AmbulanceUnitEntity();
        unit.setPosition(null);
        assertNull(unit.getLat());
        assertNull(unit.getLon());
    }
}
