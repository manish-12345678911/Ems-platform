package com.h8.ems.hospital.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.h8.ems.contracts.dto.HospitalCapacityResponse;
import com.h8.ems.contracts.dto.UpdateCapacityRequest;
import com.h8.ems.hospital.model.CapacitySnapshotEntity;
import com.h8.ems.hospital.model.HospitalEntity;
import com.h8.ems.hospital.repository.CapacitySnapshotRepository;
import com.h8.ems.hospital.repository.HospitalRepository;
import com.h8.ems.hospital.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HospitalCapacityServiceTest {

    private HospitalRepository hospitalRepository;
    private CapacitySnapshotRepository snapshotRepository;
    private OutboxRepository outboxRepository;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private HospitalCapacityService capacityService;

    private final GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        hospitalRepository = mock(HospitalRepository.class);
        snapshotRepository = mock(CapacitySnapshotRepository.class);
        outboxRepository = mock(OutboxRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        capacityService = new HospitalCapacityService(
                hospitalRepository,
                snapshotRepository,
                outboxRepository,
                redisTemplate,
                objectMapper,
                15
        );
    }

    @Test
    void testUpdateCapacityPersistsSnapshotAndCachesInRedis() {
        UUID hospitalId = UUID.randomUUID();
        Point loc = gf.createPoint(new Coordinate(77.2090, 28.6139));
        HospitalEntity hospital = new HospitalEntity(hospitalId, "City General", loc, Set.of());

        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));

        UpdateCapacityRequest req = new UpdateCapacityRequest(10, 4, 2);
        HospitalCapacityResponse res = capacityService.updateCapacity(hospitalId, req);

        assertNotNull(res);
        assertEquals(10, res.edBedsFree());
        assertEquals(4, res.icuBedsFree());
        assertEquals(2, res.ventilatorsFree());
        assertFalse(res.isStale());

        verify(snapshotRepository, times(1)).save(any(CapacitySnapshotEntity.class));
        verify(valueOperations, times(1)).set(
                eq(HospitalCapacityService.CAPACITY_KEY_PREFIX + hospitalId),
                anyString(),
                any(Duration.class)
        );
    }

    @Test
    void testGetCapacityFromLatestSnapshot() {
        UUID hospitalId = UUID.randomUUID();
        Point loc = gf.createPoint(new Coordinate(77.2090, 28.6139));
        HospitalEntity hospital = new HospitalEntity(hospitalId, "City General", loc, Set.of());

        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
        when(valueOperations.get(anyString())).thenReturn(null); // Redis cache miss

        CapacitySnapshotEntity snap = new CapacitySnapshotEntity(
                UUID.randomUUID(), hospitalId, 6, 2, 1, Instant.now()
        );
        when(snapshotRepository.findFirstByHospitalIdOrderByUpdatedAtDesc(hospitalId))
                .thenReturn(Optional.of(snap));

        HospitalCapacityResponse res = capacityService.getCapacity(hospitalId);
        assertNotNull(res);
        assertEquals(6, res.edBedsFree());
        assertFalse(res.isStale());
    }
}
