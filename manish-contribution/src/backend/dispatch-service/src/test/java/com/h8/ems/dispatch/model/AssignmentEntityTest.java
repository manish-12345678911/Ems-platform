package com.h8.ems.dispatch.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AssignmentEntityTest {

    @Test
    void defaultsAreCorrect() {
        AssignmentEntity a = new AssignmentEntity();
        assertFalse(a.isRejected());
    }

    @Test
    void fullConstructorSetsAllFields() {
        UUID id = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        Instant now = Instant.now();

        AmbulanceUnitEntity unit = new AmbulanceUnitEntity();
        unit.setId(UUID.randomUUID());

        AssignmentEntity a = new AssignmentEntity(
                id, incidentId, unit, "{\"score\":0.42}", "AUTO", now, false
        );

        assertEquals(id, a.getId());
        assertEquals(incidentId, a.getIncidentId());
        assertEquals(unit, a.getUnit());
        assertEquals("{\"score\":0.42}", a.getRankedSnapshot());
        assertEquals("AUTO", a.getChosenBy());
        assertEquals(now, a.getDecidedAt());
        assertFalse(a.isRejected());
    }

    @Test
    void rejectedCanBeUpdated() {
        AssignmentEntity a = new AssignmentEntity();
        assertFalse(a.isRejected());
        a.setRejected(true);
        assertTrue(a.isRejected());
    }
}
