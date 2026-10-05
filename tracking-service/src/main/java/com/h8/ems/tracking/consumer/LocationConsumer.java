package com.h8.ems.tracking.consumer;

import com.h8.ems.contracts.events.LocationUpdate;
import com.h8.ems.tracking.service.TrackingRedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka listener for GPS location updates from crew devices.
 * Topic: unit.location
 */
@Component
public class LocationConsumer {

    private static final Logger log = LoggerFactory.getLogger(LocationConsumer.class);

    private final TrackingRedisService trackingRedisService;

    public LocationConsumer(TrackingRedisService trackingRedisService) {
        this.trackingRedisService = trackingRedisService;
    }

    @KafkaListener(topics = "unit.location", groupId = "tracking-service")
    public void onLocationUpdate(LocationUpdate update) {
        log.debug("Received location update for unit {}: lat={}, lon={}, epochMs={}",
                update.unitId(), update.lat(), update.lon(), update.epochMs());
        trackingRedisService.updateLocation(update);
    }
}
