package com.h8.ems.tracking.pruner;

import com.h8.ems.contracts.events.UnitStatusEvent;
import com.h8.ems.tracking.service.TrackingRedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled job to prune inactive unit heartbeats from Redis GEO
 * and notify downstream services via unit.status event (to = OFFLINE).
 */
@Component
public class TrackingPruner {

    private static final Logger log = LoggerFactory.getLogger(TrackingPruner.class);

    private final TrackingRedisService trackingRedisService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final long heartbeatTtlSeconds;

    public TrackingPruner(TrackingRedisService trackingRedisService,
                          KafkaTemplate<String, Object> kafkaTemplate,
                          @Value("${h8.tracking.heartbeat-ttl-seconds:90}") long heartbeatTtlSeconds) {
        this.trackingRedisService = trackingRedisService;
        this.kafkaTemplate = kafkaTemplate;
        this.heartbeatTtlSeconds = heartbeatTtlSeconds;
    }

    @Scheduled(fixedDelayString = "${h8.tracking.pruner-interval-seconds:30}000")
    public void pruneExpiredUnits() {
        List<UUID> expiredUnits = trackingRedisService.pruneExpiredUnits(heartbeatTtlSeconds);
        if (expiredUnits.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        for (UUID unitId : expiredUnits) {
            UnitStatusEvent event = new UnitStatusEvent(
                    UUID.randomUUID(),
                    unitId,
                    "AVAILABLE",
                    "OFFLINE",
                    now
            );
            kafkaTemplate.send("unit.status", unitId.toString(), event)
                    .whenComplete((res, ex) -> {
                        if (ex == null) {
                            log.info("Emitted OFFLINE status event for expired unit {}", unitId);
                        } else {
                            log.warn("Failed to emit OFFLINE event for unit {}: {}", unitId, ex.getMessage());
                        }
                    });
        }
    }
}
