package com.h8.ems.redeployment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h8.ems.common.model.GeoPoint;
import com.h8.ems.common.model.UnitSnapshot;
import com.h8.ems.common.model.UnitStatus;
import com.h8.ems.common.model.UnitType;
import com.h8.ems.common.scoring.CoverageModel;
import com.h8.ems.common.scoring.RedeploymentPlanner;
import com.h8.ems.contracts.dto.RedeploySuggestionDto;
import com.h8.ems.redeployment.config.RedeploymentProperties;
import com.h8.ems.redeployment.model.OutboxEventEntity;
import com.h8.ems.redeployment.model.RedeployMoveEntity;
import com.h8.ems.redeployment.model.ZoneEntity;
import com.h8.ems.redeployment.repository.OutboxRepository;
import com.h8.ems.redeployment.repository.RedeployMoveRepository;
import com.h8.ems.redeployment.repository.ZoneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class RedeploymentService {

    private static final Logger log = LoggerFactory.getLogger(RedeploymentService.class);
    public static final String LOCK_KEY = "lock:redeploy";
    public static final String TOPIC_SUGGESTIONS = "redeploy.suggestions";
    public static final String TOPIC_MOVES = "redeploy.moves";

    private final ZoneRepository zoneRepository;
    private final RedeployMoveRepository moveRepository;
    private final OutboxRepository outboxRepository;
    private final StringRedisTemplate redisTemplate;
    private final RedeploymentProperties properties;
    private final ObjectMapper objectMapper;
    private final String instanceId = UUID.randomUUID().toString();

    // Cache of available units for planning
    private final Map<UUID, UnitSnapshot> knownUnits = new HashMap<>();

    public RedeploymentService(ZoneRepository zoneRepository,
                               RedeployMoveRepository moveRepository,
                               OutboxRepository outboxRepository,
                               StringRedisTemplate redisTemplate,
                               RedeploymentProperties properties,
                               ObjectMapper objectMapper) {
        this.zoneRepository = zoneRepository;
        this.moveRepository = moveRepository;
        this.outboxRepository = outboxRepository;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        ObjectMapper mapper = objectMapper != null ? objectMapper.copy() : new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper = mapper;
    }

    /**
     * Executes redeployment planning under a Redis distributed lock.
     * Hard Rule #4 (outbox) & Correction #1/2 (CoverageModel and UnitSnapshot copies).
     * Returns true if lock was acquired and plan executed, false if lock was busy.
     */
    public boolean runPlanningWithLock() {
        return runPlanningWithLock(getAvailableUnits());
    }

    public boolean runPlanningWithLock(List<UnitSnapshot> availableUnits) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                LOCK_KEY,
                instanceId,
                Duration.ofSeconds(properties.getLockTtlSeconds())
        );

        if (!Boolean.TRUE.equals(acquired)) {
            log.info("Redeployment lock busy. Another instance is currently planning. Skipping run.");
            return false;
        }

        try {
            log.info("Acquired redeployment lock. Executing planning run with {} available units.", availableUnits.size());
            executePlanning(availableUnits);
            return true;
        } finally {
            try {
                String currentOwner = redisTemplate.opsForValue().get(LOCK_KEY);
                if (instanceId.equals(currentOwner)) {
                    redisTemplate.delete(LOCK_KEY);
                }
            } catch (Exception e) {
                log.warn("Failed to cleanly release redeployment lock: {}", e.getMessage());
            }
        }
    }

    @Scheduled(fixedRateString = "${h8.redeployment.schedule-rate-ms:60000}", initialDelay = 10000)
    public void scheduledRun() {
        runPlanningWithLock();
    }

    @Transactional
    public List<RedeployMoveEntity> executePlanning(List<UnitSnapshot> availableUnits) {
        List<GeoPoint> centroids = loadZoneCentroids();
        if (centroids.isEmpty()) {
            log.warn("No demand zones configured. Cannot compute coverage.");
            return Collections.emptyList();
        }

        CoverageModel coverageModel = new CoverageModel(properties.getCoverageRadiusKm(), centroids);
        RedeploymentPlanner planner = new RedeploymentPlanner(coverageModel);

        RedeploymentPlanner.PlannerLimits limits = new RedeploymentPlanner.PlannerLimits(
                properties.getMaxMoves(),
                properties.getMinCoverageGain(),
                properties.getCooldownMinutes()
        );

        // Candidate standby points: zone centroids
        List<RedeploymentPlanner.RedeployMove> suggestedMoves = planner.plan(availableUnits, centroids, limits);
        List<RedeployMoveEntity> savedEntities = new ArrayList<>();

        Instant now = Instant.now();
        Instant cooldownThreshold = now.minus(Duration.ofMinutes(properties.getCooldownMinutes()));

        for (RedeploymentPlanner.RedeployMove move : suggestedMoves) {
            // Check move cooldown for unit
            Optional<RedeployMoveEntity> lastMove = moveRepository.findTopByUnitIdOrderByAtDesc(move.unitId());
            if (lastMove.isPresent() && lastMove.get().getAt().isAfter(cooldownThreshold)) {
                log.info("Skipping unit {} due to cooldown (last moved at {})", move.unitId(), lastMove.get().getAt());
                continue;
            }

            String targetStr = String.format(Locale.ROOT, "%.6f,%.6f", move.target().lat(), move.target().lon());
            RedeployMoveEntity entity = new RedeployMoveEntity(
                    null,
                    move.unitId(),
                    targetStr,
                    move.coverageGain(),
                    false,
                    now
            );
            entity = moveRepository.save(entity);
            savedEntities.add(entity);

            // Outbox event (Hard Rule #4)
            RedeploySuggestionDto dto = new RedeploySuggestionDto(
                    entity.getId(),
                    entity.getUnitId(),
                    move.callSign(),
                    entity.getTarget(),
                    entity.getCoverageGain(),
                    entity.isAccepted(),
                    entity.getAt()
            );

            try {
                String payload = objectMapper.writeValueAsString(dto);
                OutboxEventEntity outbox = new OutboxEventEntity(
                        entity.getId(),
                        TOPIC_SUGGESTIONS,
                        entity.getUnitId().toString(),
                        payload
                );
                outboxRepository.save(outbox);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize redeploy suggestion outbox event for unit {}", move.unitId(), e);
            }
        }

        log.info("Redeployment planning complete. Generated {} moves.", savedEntities.size());
        return savedEntities;
    }

    @Transactional
    public Optional<RedeployMoveEntity> acceptMove(UUID moveId) {
        Optional<RedeployMoveEntity> opt = moveRepository.findById(moveId);
        if (opt.isEmpty()) return Optional.empty();

        RedeployMoveEntity entity = opt.get();
        entity.setAccepted(true);
        moveRepository.save(entity);

        // Outbox event for accepted move
        try {
            RedeploySuggestionDto dto = new RedeploySuggestionDto(
                    entity.getId(),
                    entity.getUnitId(),
                    null,
                    entity.getTarget(),
                    entity.getCoverageGain(),
                    true,
                    entity.getAt()
            );
            OutboxEventEntity outbox = new OutboxEventEntity(
                    entity.getId(),
                    TOPIC_MOVES,
                    entity.getUnitId().toString(),
                    objectMapper.writeValueAsString(dto)
            );
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize accepted move outbox event {}", moveId, e);
        }

        return Optional.of(entity);
    }

    @Transactional
    public Optional<RedeployMoveEntity> declineMove(UUID moveId) {
        Optional<RedeployMoveEntity> opt = moveRepository.findById(moveId);
        if (opt.isEmpty()) return Optional.empty();

        RedeployMoveEntity entity = opt.get();
        entity.setAccepted(false);
        moveRepository.save(entity);
        return Optional.of(entity);
    }

    public List<RedeploySuggestionDto> getPendingSuggestions() {
        return moveRepository.findByAcceptedFalseOrderByAtDesc().stream()
                .map(m -> new RedeploySuggestionDto(
                        m.getId(),
                        m.getUnitId(),
                        null,
                        m.getTarget(),
                        m.getCoverageGain(),
                        m.isAccepted(),
                        m.getAt()
                ))
                .toList();
    }

    public double calculateCurrentCoverage() {
        return calculateCurrentCoverage(getAvailableUnits());
    }

    public double calculateCurrentCoverage(List<UnitSnapshot> units) {
        List<GeoPoint> centroids = loadZoneCentroids();
        if (centroids.isEmpty()) return 1.0;
        CoverageModel model = new CoverageModel(properties.getCoverageRadiusKm(), centroids);
        return model.coverage(units);
    }

    public void registerUnit(UnitSnapshot unit) {
        knownUnits.put(unit.id(), unit);
    }

    public List<UnitSnapshot> getAvailableUnits() {
        if (!knownUnits.isEmpty()) {
            return knownUnits.values().stream()
                    .filter(u -> u.status() == UnitStatus.AVAILABLE)
                    .toList();
        }
        // Fallback default units if no units registered yet
        return List.of(
                new UnitSnapshot(UUID.randomUUID(), "AMB-01", UnitType.ALS, UnitStatus.AVAILABLE,
                        new GeoPoint(51.5074, -0.1278), Instant.now(), Instant.now(), null, null),
                new UnitSnapshot(UUID.randomUUID(), "AMB-02", UnitType.BLS, UnitStatus.AVAILABLE,
                        new GeoPoint(51.5200, -0.1000), Instant.now(), Instant.now(), null, null)
        );
    }

    private List<GeoPoint> loadZoneCentroids() {
        List<ZoneEntity> zones = zoneRepository.findAll();
        if (!zones.isEmpty()) {
            return zones.stream().map(ZoneEntity::toGeoPoint).toList();
        }
        // Baseline default zones if not populated yet
        return List.of(
                new GeoPoint(51.5074, -0.1278), // Central
                new GeoPoint(51.5155, -0.0922), // East
                new GeoPoint(51.5014, -0.1419), // West
                new GeoPoint(51.5300, -0.1200)  // North
        );
    }
}
