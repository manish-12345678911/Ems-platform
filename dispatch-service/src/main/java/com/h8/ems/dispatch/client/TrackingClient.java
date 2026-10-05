package com.h8.ems.dispatch.client;

import com.h8.ems.common.model.GeoPoint;
import com.h8.ems.common.model.UnitStatus;
import com.h8.ems.contracts.dto.NearbyUnitResponse;
import com.h8.ems.dispatch.model.AmbulanceUnitEntity;
import com.h8.ems.dispatch.repository.AmbulanceUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Client for tracking-service (Port 8083) to find nearby active units in Redis GEO.
 * Falls back to database query if tracking-service is unavailable.
 */
@Component
public class TrackingClient {

    private static final Logger log = LoggerFactory.getLogger(TrackingClient.class);

    private final RestClient restClient;
    private final AmbulanceUnitRepository unitRepository;

    public TrackingClient(@Value("${h8.tracking.url:http://localhost:8083}") String trackingUrl,
                          @Value("${h8.tracking.timeout-ms:500}") int timeoutMs,
                          AmbulanceUnitRepository unitRepository) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(timeoutMs))
                .withReadTimeout(Duration.ofMillis(timeoutMs));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);

        this.restClient = RestClient.builder()
                .baseUrl(trackingUrl)
                .requestFactory(requestFactory)
                .build();
        this.unitRepository = unitRepository;
    }

    /**
     * Finds nearby available unit IDs and distances around coordinates.
     */
    public List<NearbyUnitResponse> findNearbyUnits(double lat, double lon, double radiusKm, int limit) {
        try {
            List<NearbyUnitResponse> nearby = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tracking/nearby")
                            .queryParam("lat", lat)
                            .queryParam("lon", lon)
                            .queryParam("radiusKm", radiusKm)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<NearbyUnitResponse>>() {});

            if (nearby != null && !nearby.isEmpty()) {
                return nearby;
            }
        } catch (Exception e) {
            log.debug("Tracking-service call failed ({}), falling back to database query", e.getMessage());
        }

        // Database fallback
        return fallbackFromDatabase(lat, lon, radiusKm, limit);
    }

    private List<NearbyUnitResponse> fallbackFromDatabase(double lat, double lon, double radiusKm, int limit) {
        GeoPoint origin = new GeoPoint(lat, lon);
        List<AmbulanceUnitEntity> available = unitRepository.findByStatus(UnitStatus.AVAILABLE);

        List<NearbyUnitResponse> results = new ArrayList<>();
        for (AmbulanceUnitEntity unit : available) {
            Double uLat = unit.getLat();
            Double uLon = unit.getLon();
            if (uLat != null && uLon != null) {
                GeoPoint uPos = new GeoPoint(uLat, uLon);
                double dist = origin.distanceTo(uPos);
                if (dist <= radiusKm) {
                    long epochMs = unit.getPositionAt() != null ? unit.getPositionAt().toEpochMilli() : 0L;
                    results.add(new NearbyUnitResponse(unit.getId(), dist, uLat, uLon, epochMs));
                }
            }
        }

        results.sort(Comparator.comparingDouble(NearbyUnitResponse::distanceKm));
        if (results.size() > limit) {
            return results.subList(0, limit);
        }
        return results;
    }
}
