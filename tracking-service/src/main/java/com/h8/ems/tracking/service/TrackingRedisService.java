package com.h8.ems.tracking.service;

import com.h8.ems.contracts.dto.NearbyUnitResponse;
import com.h8.ems.contracts.events.LocationUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service managing real-time unit positions in Redis.
 * Uses atomic Lua script for compare-and-set timestamp to guard against out-of-order updates (Rule #5 & Correction #7).
 */
@Service
public class TrackingRedisService {

    private static final Logger log = LoggerFactory.getLogger(TrackingRedisService.class);

    public static final String GEO_KEY = "units:geo";
    public static final String TS_PREFIX = "unit:ts:";

    public static final String UPDATE_LOCATION_LUA = """
            local current_ts = redis.call('GET', KEYS[1])
            if not current_ts or tonumber(ARGV[1]) > tonumber(current_ts) then
                redis.call('SET', KEYS[1], ARGV[1])
                redis.call('GEOADD', KEYS[2], ARGV[2], ARGV[3], ARGV[4])
                return 1
            else
                return 0
            end
            """;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> updateScript;

    public TrackingRedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.updateScript = new DefaultRedisScript<>(UPDATE_LOCATION_LUA, Long.class);
    }

    /**
     * Atomically updates unit position if epochMs > current recorded timestamp.
     * Returns true if applied, false if rejected as out-of-order or stale.
     */
    public boolean updateLocation(LocationUpdate update) {
        String tsKey = TS_PREFIX + update.unitId();
        List<String> keys = List.of(tsKey, GEO_KEY);

        Long result = redisTemplate.execute(
                updateScript,
                keys,
                String.valueOf(update.epochMs()),
                String.valueOf(update.lon()),
                String.valueOf(update.lat()),
                update.unitId().toString()
        );

        boolean updated = result != null && result == 1L;
        if (!updated) {
            log.debug("Rejected stale/out-of-order GPS ping for unit {} (epochMs={})",
                    update.unitId(), update.epochMs());
        }
        return updated;
    }

    /**
     * Finds nearby units within radiusKm up to limit.
     */
    public List<NearbyUnitResponse> findNearbyUnits(double lat, double lon, double radiusKm, int limit) {
        Point center = new Point(lon, lat);
        Distance distance = new Distance(radiusKm, Metrics.KILOMETERS);
        org.springframework.data.redis.domain.geo.GeoReference<String> reference =
                org.springframework.data.redis.domain.geo.GeoReference.fromCoordinate(lon, lat);

        RedisGeoCommands.GeoSearchCommandArgs args = RedisGeoCommands.GeoSearchCommandArgs
                .newGeoSearchArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending()
                .limit(limit);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().search(GEO_KEY, reference, distance, args);
        if (results == null || results.getContent().isEmpty()) {
            return Collections.emptyList();
        }

        List<NearbyUnitResponse> nearbyList = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> r : results) {
            String unitIdStr = r.getContent().getName();
            UUID unitId;
            try {
                unitId = UUID.fromString(unitIdStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            double dist = r.getDistance().getValue();
            Point pt = r.getContent().getPoint();
            double unitLon = pt != null ? pt.getX() : 0.0;
            double unitLat = pt != null ? pt.getY() : 0.0;

            String tsStr = redisTemplate.opsForValue().get(TS_PREFIX + unitIdStr);
            long lastSeen = tsStr != null ? Long.parseLong(tsStr) : 0L;

            nearbyList.add(new NearbyUnitResponse(unitId, dist, unitLat, unitLon, lastSeen));
        }

        return nearbyList;
    }

    /**
     * Prunes units whose last heartbeat is older than ttlSeconds.
     * Returns the list of pruned unit IDs.
     */
    public List<UUID> pruneExpiredUnits(long ttlSeconds) {
        Set<String> allMembers = redisTemplate.opsForZSet().range(GEO_KEY, 0, -1);
        if (allMembers == null || allMembers.isEmpty()) {
            return Collections.emptyList();
        }

        long nowMs = Instant.now().toEpochMilli();
        long ttlMs = ttlSeconds * 1000L;
        List<UUID> expiredUnits = new ArrayList<>();

        for (String member : allMembers) {
            String tsStr = redisTemplate.opsForValue().get(TS_PREFIX + member);
            boolean expired = false;
            if (tsStr == null) {
                expired = true;
            } else {
                try {
                    long ts = Long.parseLong(tsStr);
                    if (nowMs - ts > ttlMs) {
                        expired = true;
                    }
                } catch (NumberFormatException e) {
                    expired = true;
                }
            }

            if (expired) {
                try {
                    UUID unitId = UUID.fromString(member);
                    redisTemplate.opsForZSet().remove(GEO_KEY, member);
                    expiredUnits.add(unitId);
                    log.info("Pruned expired unit {} from Redis GEO (inactive for > {}s)", member, ttlSeconds);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        return expiredUnits;
    }
}
