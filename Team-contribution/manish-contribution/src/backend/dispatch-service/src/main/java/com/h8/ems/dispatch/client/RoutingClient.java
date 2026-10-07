package com.h8.ems.dispatch.client;

import com.h8.ems.common.eta.EtaProvider;
import com.h8.ems.common.eta.HaversineEta;
import com.h8.ems.common.model.GeoPoint;
import com.h8.ems.contracts.dto.EtaRequest;
import com.h8.ems.contracts.dto.EtaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;

/**
 * Client for routing-service (Port 8084) with automatic fallback to HaversineEta (from common).
 */
@Component
public class RoutingClient {

    private static final Logger log = LoggerFactory.getLogger(RoutingClient.class);

    private final RestClient restClient;
    private final EtaProvider fallbackEta;

    public RoutingClient(@Value("${h8.routing.url:http://localhost:8084}") String routingUrl,
                         @Value("${h8.routing.timeout-ms:300}") int timeoutMs) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(timeoutMs))
                .withReadTimeout(Duration.ofMillis(timeoutMs));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);

        this.restClient = RestClient.builder()
                .baseUrl(routingUrl)
                .requestFactory(requestFactory)
                .build();
        this.fallbackEta = new HaversineEta();
    }

    /**
     * Calculates ETA in seconds between two points.
     * Calls routing-service HTTP /eta; falls back to HaversineEta on network or timeout failure.
     */
    public double getEtaSeconds(GeoPoint from, GeoPoint to, Instant at) {
        if (from == null || to == null) {
            return 0.0;
        }

        try {
            EtaRequest req = new EtaRequest(from.lat(), from.lon(), to.lat(), to.lon());
            EtaResponse resp = restClient.post()
                    .uri("/eta")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(EtaResponse.class);

            if (resp != null) {
                return resp.etaSeconds();
            }
        } catch (Exception e) {
            log.debug("Routing-service ETA call failed, using Haversine fallback: {}", e.getMessage());
        }

        return fallbackEta.etaSeconds(from, to, at);
    }
}
