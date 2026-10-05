package com.h8.ems.tracking.consumer;

import com.h8.ems.contracts.events.LocationUpdate;
import com.h8.ems.tracking.service.TrackingRedisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocationConsumerTest {

    @Mock
    private TrackingRedisService trackingRedisService;

    @Test
    void onLocationUpdateDelegatesToService() {
        LocationConsumer consumer = new LocationConsumer(trackingRedisService);
        UUID unitId = UUID.randomUUID();
        LocationUpdate update = new LocationUpdate(unitId, 51.50, -0.12, 1700000000000L, 25.0);

        consumer.onLocationUpdate(update);

        verify(trackingRedisService, times(1)).updateLocation(update);
    }
}
