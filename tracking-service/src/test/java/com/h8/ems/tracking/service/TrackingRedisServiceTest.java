package com.h8.ems.tracking.service;

import com.h8.ems.contracts.dto.NearbyUnitResponse;
import com.h8.ems.contracts.events.LocationUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingRedisServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private GeoOperations<String, String> geoOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private TrackingRedisService service;

    @BeforeEach
    void setUp() {
        service = new TrackingRedisService(redisTemplate);
    }

    @Test
    void updateLocationReturnsTrueWhenScriptAppliesUpdate() {
        UUID unitId = UUID.randomUUID();
        LocationUpdate update = new LocationUpdate(unitId, 51.50, -0.12, 1700000000000L, 30.0);

        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
                .thenReturn(1L);

        boolean result = service.updateLocation(update);
        assertTrue(result, "Should return true when Lua script returns 1");
    }

    @Test
    void updateLocationReturnsFalseWhenScriptRejectsStaleUpdate() {
        UUID unitId = UUID.randomUUID();
        LocationUpdate update = new LocationUpdate(unitId, 51.50, -0.12, 1600000000000L, 30.0);

        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
                .thenReturn(0L);

        boolean result = service.updateLocation(update);
        assertFalse(result, "Should return false when Lua script rejects out-of-order ping");
    }

    @Test
    void findNearbyUnitsReturnsMappedResults() {
        UUID unitId = UUID.randomUUID();
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        RedisGeoCommands.GeoLocation<String> location = new RedisGeoCommands.GeoLocation<>(
                unitId.toString(), new Point(-0.12, 51.50)
        );
        GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult = new GeoResult<>(location, new Distance(3.5, Metrics.KILOMETERS));
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = new GeoResults<>(List.of(geoResult));

        when(geoOperations.search(eq(TrackingRedisService.GEO_KEY), any(org.springframework.data.redis.domain.geo.GeoReference.class), any(Distance.class), any(RedisGeoCommands.GeoSearchCommandArgs.class)))
                .thenReturn(geoResults);
        when(valueOperations.get(TrackingRedisService.TS_PREFIX + unitId.toString()))
                .thenReturn("1700000000000");

        List<NearbyUnitResponse> results = service.findNearbyUnits(51.50, -0.12, 10.0, 5);

        assertEquals(1, results.size());
        NearbyUnitResponse resp = results.getFirst();
        assertEquals(unitId, resp.unitId());
        assertEquals(3.5, resp.distanceKm());
        assertEquals(51.50, resp.lat());
        assertEquals(-0.12, resp.lon());
        assertEquals(1700000000000L, resp.lastSeenEpochMs());
    }

    @Test
    void pruneExpiredUnitsRemovesStaleMembers() {
        UUID staleUnitId = UUID.randomUUID();
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(zSetOperations.range(TrackingRedisService.GEO_KEY, 0, -1))
                .thenReturn(Set.of(staleUnitId.toString()));
        // Simulated timestamp from 10 minutes ago
        long oldTs = System.currentTimeMillis() - (600 * 1000L);
        when(valueOperations.get(TrackingRedisService.TS_PREFIX + staleUnitId.toString()))
                .thenReturn(String.valueOf(oldTs));

        List<UUID> pruned = service.pruneExpiredUnits(90);

        assertEquals(1, pruned.size());
        assertEquals(staleUnitId, pruned.getFirst());
        verify(zSetOperations, times(1)).remove(TrackingRedisService.GEO_KEY, staleUnitId.toString());
    }
}
