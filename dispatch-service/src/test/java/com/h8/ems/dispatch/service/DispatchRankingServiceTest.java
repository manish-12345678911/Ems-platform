package com.h8.ems.dispatch.service;

import com.h8.ems.common.model.*;
import com.h8.ems.contracts.dto.CandidateRankingResponse;
import com.h8.ems.contracts.dto.NearbyUnitResponse;
import com.h8.ems.dispatch.client.RoutingClient;
import com.h8.ems.dispatch.client.TrackingClient;
import com.h8.ems.dispatch.model.AmbulanceUnitEntity;
import com.h8.ems.dispatch.model.StationEntity;
import com.h8.ems.dispatch.repository.AmbulanceUnitRepository;
import com.h8.ems.dispatch.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchRankingServiceTest {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Mock private AmbulanceUnitRepository unitRepository;
    @Mock private StationRepository stationRepository;
    @Mock private TrackingClient trackingClient;
    @Mock private RoutingClient routingClient;

    private DispatchRankingService rankingService;

    @BeforeEach
    void setUp() {
        rankingService = new DispatchRankingService(
                unitRepository, stationRepository, trackingClient, routingClient,
                25.0, 15, 50.0, 100.0
        );
    }

    private AmbulanceUnitEntity makeUnit(UUID id, String callSign, UnitType type,
                                          double lon, double lat) {
        Point pos = GF.createPoint(new Coordinate(lon, lat));
        return new AmbulanceUnitEntity(
                id, callSign, type, UnitStatus.AVAILABLE,
                pos, Instant.now(), Instant.now().minusSeconds(3600), null
        );
    }

    @Test
    void rankCandidatesReturnsScoresSortedAscending() {
        UUID unit1Id = UUID.randomUUID();
        UUID unit2Id = UUID.randomUUID();
        Instant now = Instant.now();

        // tracking returns two nearby units
        when(trackingClient.findNearbyUnits(anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(
                        new NearbyUnitResponse(unit1Id, 5.0, 51.50, -0.12, now.toEpochMilli()),
                        new NearbyUnitResponse(unit2Id, 10.0, 51.51, -0.13, now.toEpochMilli())
                ));

        AmbulanceUnitEntity u1 = makeUnit(unit1Id, "ALS-1", UnitType.ALS, -0.12, 51.50);
        AmbulanceUnitEntity u2 = makeUnit(unit2Id, "BLS-1", UnitType.BLS, -0.13, 51.51);

        when(unitRepository.findByIdInAndStatus(anyCollection(), eq(UnitStatus.AVAILABLE)))
                .thenReturn(List.of(u1, u2));
        when(unitRepository.findByStatus(UnitStatus.AVAILABLE))
                .thenReturn(List.of(u1, u2));
        when(stationRepository.findAll()).thenReturn(List.of());

        // ALS unit is closer
        when(routingClient.getEtaSeconds(any(), any(), any()))
                .thenReturn(300.0)  // u1: 5 min
                .thenReturn(600.0); // u2: 10 min

        IncidentSnapshot incident = new IncidentSnapshot(
                UUID.randomUUID(),
                new GeoPoint(51.50, -0.12),
                Severity.EMERGENCY,
                ClinicalNeed.TRAUMA,
                false,
                IncidentStatus.RECEIVED,
                now
        );

        List<CandidateRankingResponse> candidates = rankingService.rankCandidates(incident, now);

        assertFalse(candidates.isEmpty());
        assertEquals(2, candidates.size());
        // Sorted ascending by score (lower = better)
        assertTrue(candidates.get(0).score() <= candidates.get(1).score());
    }

    @Test
    void rankCandidatesReturnsEmptyWhenNoNearbyUnits() {
        when(trackingClient.findNearbyUnits(anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of());

        IncidentSnapshot incident = new IncidentSnapshot(
                UUID.randomUUID(),
                new GeoPoint(51.50, -0.12),
                Severity.URGENT,
                ClinicalNeed.GENERAL,
                false,
                IncidentStatus.RECEIVED,
                Instant.now()
        );

        List<CandidateRankingResponse> candidates = rankingService.rankCandidates(incident, Instant.now());
        assertTrue(candidates.isEmpty());
    }

    @Test
    void rankCandidatesReturnsEmptyWhenNullLocation() {
        IncidentSnapshot incident = new IncidentSnapshot(
                UUID.randomUUID(),
                null,
                Severity.URGENT,
                ClinicalNeed.GENERAL,
                false,
                IncidentStatus.RECEIVED,
                Instant.now()
        );

        List<CandidateRankingResponse> candidates = rankingService.rankCandidates(incident, Instant.now());
        assertTrue(candidates.isEmpty());
    }

    @Test
    void alsRequiredPenalizesBLSUnits() {
        UUID alsId = UUID.randomUUID();
        UUID blsId = UUID.randomUUID();
        Instant now = Instant.now();

        when(trackingClient.findNearbyUnits(anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(
                        new NearbyUnitResponse(alsId, 10.0, 51.50, -0.12, now.toEpochMilli()),
                        new NearbyUnitResponse(blsId, 5.0, 51.50, -0.11, now.toEpochMilli())
                ));

        AmbulanceUnitEntity als = makeUnit(alsId, "ALS-1", UnitType.ALS, -0.12, 51.50);
        AmbulanceUnitEntity bls = makeUnit(blsId, "BLS-1", UnitType.BLS, -0.11, 51.50);

        when(unitRepository.findByIdInAndStatus(anyCollection(), eq(UnitStatus.AVAILABLE)))
                .thenReturn(List.of(als, bls));
        when(unitRepository.findByStatus(UnitStatus.AVAILABLE))
                .thenReturn(List.of(als, bls));
        when(stationRepository.findAll()).thenReturn(List.of());

        // BLS is closer but ALS is required
        when(routingClient.getEtaSeconds(any(), any(), any()))
                .thenReturn(600.0)  // ALS: 10 min ETA
                .thenReturn(300.0); // BLS: 5 min ETA

        IncidentSnapshot incident = new IncidentSnapshot(
                UUID.randomUUID(),
                new GeoPoint(51.50, -0.12),
                Severity.CRITICAL,
                ClinicalNeed.CARDIAC,
                true,  // requiresAls!
                IncidentStatus.RECEIVED,
                now
        );

        List<CandidateRankingResponse> candidates = rankingService.rankCandidates(incident, now);

        assertEquals(2, candidates.size());
        // ALS should rank higher (lower score) even though farther, due to BLS capability penalty
        CandidateRankingResponse top = candidates.get(0);
        assertEquals("ALS", top.type());
    }
}
