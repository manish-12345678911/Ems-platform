package com.h8.ems.redeployment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h8.ems.common.model.GeoPoint;
import com.h8.ems.common.model.UnitSnapshot;
import com.h8.ems.common.model.UnitStatus;
import com.h8.ems.common.model.UnitType;
import com.h8.ems.redeployment.config.RedeploymentProperties;
import com.h8.ems.redeployment.model.OutboxEventEntity;
import com.h8.ems.redeployment.model.RedeployMoveEntity;
import com.h8.ems.redeployment.model.ZoneEntity;
import com.h8.ems.redeployment.repository.OutboxRepository;
import com.h8.ems.redeployment.repository.RedeployMoveRepository;
import com.h8.ems.redeployment.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedeploymentServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private RedeployMoveRepository moveRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedeploymentProperties properties;
    private ObjectMapper objectMapper;
    private RedeploymentService service;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @BeforeEach
    void setUp() {
        properties = new RedeploymentProperties();
        properties.setMaxMoves(3);
        properties.setMinCoverageGain(0.01);
        properties.setCooldownMinutes(10);
        properties.setCoverageRadiusKm(5.0);
        properties.setLockTtlSeconds(30);

        objectMapper = new ObjectMapper();

        service = new RedeploymentService(
                zoneRepository,
                moveRepository,
                outboxRepository,
                redisTemplate,
                properties,
                objectMapper
        );
    }

    @Test
    @DisplayName("executePlanning creates moves and writes outbox events")
    void testExecutePlanningSuccess() {
        Point p1 = geometryFactory.createPoint(new Coordinate(-0.1278, 51.5074));
        Point p2 = geometryFactory.createPoint(new Coordinate(-0.0922, 51.5155));
        when(zoneRepository.findAll()).thenReturn(List.of(
                new ZoneEntity(1, p1, 10.0),
                new ZoneEntity(2, p2, 10.0)
        ));

        UUID unitId = UUID.randomUUID();
        // Unit is far from all zones, moving to zone 1 gives huge coverage gain
        UnitSnapshot unit = new UnitSnapshot(
                unitId, "AMB-01", UnitType.ALS, UnitStatus.AVAILABLE,
                new GeoPoint(51.6000, -0.3000), Instant.now(), Instant.now(), null, null
        );

        when(moveRepository.findTopByUnitIdOrderByAtDesc(unitId)).thenReturn(Optional.empty());
        when(moveRepository.save(any(RedeployMoveEntity.class))).thenAnswer(invocation -> {
            RedeployMoveEntity e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        List<RedeployMoveEntity> moves = service.executePlanning(List.of(unit));

        assertThat(moves).isNotEmpty();
        verify(moveRepository, atLeastOnce()).save(any(RedeployMoveEntity.class));
        verify(outboxRepository, atLeastOnce()).save(argThat(outbox ->
                outbox.getTopic().equals("redeploy.suggestions") &&
                outbox.getEventKey().equals(unitId.toString())
        ));
    }

    @Test
    @DisplayName("executePlanning respects cooldown window")
    void testExecutePlanningCooldown() {
        Point p1 = geometryFactory.createPoint(new Coordinate(-0.1278, 51.5074));
        when(zoneRepository.findAll()).thenReturn(List.of(new ZoneEntity(1, p1, 10.0)));

        UUID unitId = UUID.randomUUID();
        UnitSnapshot unit = new UnitSnapshot(
                unitId, "AMB-01", UnitType.ALS, UnitStatus.AVAILABLE,
                new GeoPoint(51.6000, -0.3000), Instant.now(), Instant.now(), null, null
        );

        // Recent move 2 minutes ago (within 10-minute cooldown)
        RedeployMoveEntity recentMove = new RedeployMoveEntity(
                UUID.randomUUID(), unitId, "51.5074,-0.1278", 0.5, true, Instant.now().minus(Duration.ofMinutes(2))
        );
        when(moveRepository.findTopByUnitIdOrderByAtDesc(unitId)).thenReturn(Optional.of(recentMove));

        List<RedeployMoveEntity> moves = service.executePlanning(List.of(unit));

        assertThat(moves).isEmpty();
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("acceptMove sets accepted flag and publishes to outbox")
    void testAcceptMove() {
        UUID moveId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        RedeployMoveEntity move = new RedeployMoveEntity(
                moveId, unitId, "51.5074,-0.1278", 0.25, false, Instant.now()
        );
        when(moveRepository.findById(moveId)).thenReturn(Optional.of(move));
        when(moveRepository.save(any(RedeployMoveEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<RedeployMoveEntity> accepted = service.acceptMove(moveId);

        assertThat(accepted).isPresent();
        assertThat(accepted.get().isAccepted()).isTrue();
        verify(outboxRepository).save(argThat(outbox ->
                outbox.getTopic().equals("redeploy.moves") &&
                outbox.getEventKey().equals(unitId.toString())
        ));
    }

    @Test
    @DisplayName("calculateCurrentCoverage computes fraction of covered zones")
    void testCoverageCalculation() {
        Point p1 = geometryFactory.createPoint(new Coordinate(-0.1278, 51.5074));
        when(zoneRepository.findAll()).thenReturn(List.of(new ZoneEntity(1, p1, 10.0)));

        // Unit directly at the zone centroid -> coverage 1.0 (100%)
        UnitSnapshot unit = new UnitSnapshot(
                UUID.randomUUID(), "AMB-01", UnitType.ALS, UnitStatus.AVAILABLE,
                new GeoPoint(51.5074, -0.1278), Instant.now(), Instant.now(), null, null
        );

        double coverage = service.calculateCurrentCoverage(List.of(unit));
        assertThat(coverage).isEqualTo(1.0);
    }
}
