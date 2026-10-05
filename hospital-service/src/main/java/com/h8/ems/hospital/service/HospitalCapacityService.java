package com.h8.ems.hospital.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h8.ems.contracts.dto.HospitalCapacityResponse;
import com.h8.ems.contracts.dto.UpdateCapacityRequest;
import com.h8.ems.hospital.model.CapacitySnapshotEntity;
import com.h8.ems.hospital.model.HospitalEntity;
import com.h8.ems.hospital.model.OutboxEventEntity;
import com.h8.ems.hospital.repository.CapacitySnapshotRepository;
import com.h8.ems.hospital.repository.HospitalRepository;
import com.h8.ems.hospital.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Service managing hospital capacity updates in Redis and PostgreSQL.
 * Real-time capacity is stored in Redis with a TTL (stale after TTL per Rule #5).
 * Historical snapshots are persisted to PostgreSQL capacity_snapshot table.
 */
@Service
public class HospitalCapacityService {

    private static final Logger log = LoggerFactory.getLogger(HospitalCapacityService.class);
    public static final String CAPACITY_KEY_PREFIX = "hospital:capacity:";

    private final HospitalRepository hospitalRepository;
    private final CapacitySnapshotRepository snapshotRepository;
    private final OutboxRepository outboxRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int capacityTtlMinutes;

    public HospitalCapacityService(
            HospitalRepository hospitalRepository,
            CapacitySnapshotRepository snapshotRepository,
            OutboxRepository outboxRepository,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${h8.hospital.capacity-ttl-minutes:15}") int capacityTtlMinutes) {
        this.hospitalRepository = hospitalRepository;
        this.snapshotRepository = snapshotRepository;
        this.outboxRepository = outboxRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.capacityTtlMinutes = capacityTtlMinutes;
    }

    public int getCapacityTtlMinutes() {
        return capacityTtlMinutes;
    }

    @Transactional
    public HospitalCapacityResponse updateCapacity(UUID hospitalId, UpdateCapacityRequest request) {
        HospitalEntity hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found: " + hospitalId));

        Instant now = Instant.now();

        // 1. Persist snapshot to PostgreSQL
        CapacitySnapshotEntity snapshot = new CapacitySnapshotEntity(
                null,
                hospitalId,
                request.edBedsFree(),
                request.icuBedsFree(),
                request.ventilatorsFree(),
                now
        );
        snapshotRepository.save(snapshot);

        // 2. Cache in Redis with TTL
        HospitalCapacityResponse response = new HospitalCapacityResponse(
                hospitalId,
                hospital.getName(),
                request.edBedsFree(),
                request.icuBedsFree(),
                request.ventilatorsFree(),
                now,
                false
        );

        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(
                    CAPACITY_KEY_PREFIX + hospitalId,
                    json,
                    Duration.ofMinutes(capacityTtlMinutes)
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize capacity to Redis for hospital {}: {}", hospitalId, e.getMessage());
        }

        // 3. Publish outbox event
        try {
            outboxRepository.save(new OutboxEventEntity(
                    null,
                    hospitalId,
                    "hospital.capacity",
                    hospitalId.toString(),
                    objectMapper.writeValueAsString(response),
                    now,
                    null
            ));
        } catch (JsonProcessingException e) {
            log.error("Failed to write outbox event for capacity update: {}", e.getMessage());
        }

        return response;
    }

    public HospitalCapacityResponse getCapacity(UUID hospitalId) {
        HospitalEntity hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found: " + hospitalId));

        // 1. Try Redis cache
        String json = redisTemplate.opsForValue().get(CAPACITY_KEY_PREFIX + hospitalId);
        if (json != null) {
            try {
                return objectMapper.readValue(json, HospitalCapacityResponse.class);
            } catch (JsonProcessingException ignored) {}
        }

        // 2. Fall back to latest PostgreSQL snapshot (check staleness per Rule #5)
        Optional<CapacitySnapshotEntity> latestOpt = snapshotRepository.findFirstByHospitalIdOrderByUpdatedAtDesc(hospitalId);
        if (latestOpt.isPresent()) {
            CapacitySnapshotEntity s = latestOpt.get();
            boolean isStale = s.getUpdatedAt().plus(Duration.ofMinutes(capacityTtlMinutes)).isBefore(Instant.now());
            return new HospitalCapacityResponse(
                    hospitalId,
                    hospital.getName(),
                    s.getEdBedsFree(),
                    s.getIcuBedsFree(),
                    s.getVentilatorsFree(),
                    s.getUpdatedAt(),
                    isStale
            );
        }

        // Unknown capacity is considered stale
        return new HospitalCapacityResponse(
                hospitalId,
                hospital.getName(),
                0, 0, 0,
                null,
                true
        );
    }
}
