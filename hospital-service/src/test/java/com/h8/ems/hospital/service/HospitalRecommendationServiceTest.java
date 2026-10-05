package com.h8.ems.hospital.service;

import com.h8.ems.common.model.ClinicalNeed;
import com.h8.ems.common.model.Severity;
import com.h8.ems.contracts.dto.HospitalCapacityResponse;
import com.h8.ems.contracts.dto.HospitalRecommendationResponse;
import com.h8.ems.hospital.model.HospitalEntity;
import com.h8.ems.hospital.repository.HospitalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HospitalRecommendationServiceTest {

    private HospitalRepository hospitalRepository;
    private HospitalCapacityService capacityService;
    private HospitalRecommendationService recommendationService;
    private final GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    void setUp() {
        hospitalRepository = mock(HospitalRepository.class);
        capacityService = mock(HospitalCapacityService.class);
        when(capacityService.getCapacityTtlMinutes()).thenReturn(15);
        recommendationService = new HospitalRecommendationService(hospitalRepository, capacityService, "http://localhost:8084");
    }

    @Test
    void testRecommendationMatchesCapabilityAndRanks() {
        UUID h1Id = UUID.randomUUID();
        UUID h2Id = UUID.randomUUID();

        Point p1 = gf.createPoint(new Coordinate(77.2090, 28.6139));
        Point p2 = gf.createPoint(new Coordinate(77.2500, 28.6500));

        // H1 supports TRAUMA and CARDIAC, H2 supports only PEDIATRIC
        HospitalEntity h1 = new HospitalEntity(h1Id, "Trauma Hospital", p1, Set.of(ClinicalNeed.TRAUMA, ClinicalNeed.CARDIAC));
        HospitalEntity h2 = new HospitalEntity(h2Id, "Children Hospital", p2, Set.of(ClinicalNeed.PEDIATRIC));

        when(hospitalRepository.findAll()).thenReturn(List.of(h1, h2));
        when(capacityService.getCapacity(h1Id)).thenReturn(new HospitalCapacityResponse(
                h1Id, "Trauma Hospital", 5, 2, 1, Instant.now(), false
        ));
        when(capacityService.getCapacity(h2Id)).thenReturn(new HospitalCapacityResponse(
                h2Id, "Children Hospital", 4, 1, 0, Instant.now(), false
        ));

        // Recommend for TRAUMA patient
        List<HospitalRecommendationResponse> recs = recommendationService.recommendDestinations(
                28.6139, 77.2090, "TRAUMA", Severity.URGENT
        );

        // H2 should be filtered out because it does not support TRAUMA
        assertEquals(1, recs.size());
        assertEquals(h1Id, recs.get(0).hospitalId());
        assertEquals("Trauma Hospital", recs.get(0).name());
        assertFalse(recs.get(0).capacityStale());
    }

    @Test
    void testStaleCapacityPenalized() {
        UUID h1Id = UUID.randomUUID();
        Point p1 = gf.createPoint(new Coordinate(77.2090, 28.6139));

        HospitalEntity h1 = new HospitalEntity(h1Id, "Stale Hospital", p1, Set.of(ClinicalNeed.GENERAL));
        when(hospitalRepository.findAll()).thenReturn(List.of(h1));

        // Stale capacity (updated 2 hours ago)
        Instant twoHoursAgo = Instant.now().minusSeconds(7200);
        when(capacityService.getCapacity(h1Id)).thenReturn(new HospitalCapacityResponse(
                h1Id, "Stale Hospital", 0, 0, 0, twoHoursAgo, true
        ));

        List<HospitalRecommendationResponse> recs = recommendationService.recommendDestinations(
                28.6139, 77.2090, "GENERAL", Severity.URGENT
        );

        assertEquals(1, recs.size());
        assertTrue(recs.get(0).capacityStale());
    }
}
