package com.h8.ems.routing.service;

import com.h8.ems.common.eta.EtaProvider;
import com.h8.ems.common.eta.HaversineEta;
import com.h8.ems.common.model.GeoPoint;
import com.h8.ems.contracts.dto.EtaRequest;
import com.h8.ems.contracts.dto.EtaResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Calculates ETA for routing requests.
 * Protected with Resilience4j circuit breaker with fallback to HaversineEta.
 */
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Calculates ETA for routing requests.
 * Uses GraphHopperEta as primary provider, protected with Resilience4j circuit breaker with fallback to HaversineEta.
 */
@Service
public class RoutingEtaService {

    private static final Logger log = LoggerFactory.getLogger(RoutingEtaService.class);

    private final EtaProvider primaryProvider;
    private final EtaProvider fallbackProvider;

    public RoutingEtaService() {
        this(null, new HaversineEta());
    }

    @Autowired
    public RoutingEtaService(@Autowired(required = false) GraphHopperEta primaryProvider) {
        this(primaryProvider, new HaversineEta());
    }

    public RoutingEtaService(EtaProvider primaryProvider, EtaProvider fallbackProvider) {
        this.primaryProvider = primaryProvider;
        this.fallbackProvider = fallbackProvider != null ? fallbackProvider : new HaversineEta();
    }

    @CircuitBreaker(name = "routingEta", fallbackMethod = "calculateFallback")
    public EtaResponse calculateEta(EtaRequest request) {
        boolean usePrimary = primaryProvider != null;
        if (primaryProvider instanceof GraphHopperEta gh && !gh.isLoaded()) {
            usePrimary = false;
        }

        if (!usePrimary) {
            double eta = fallbackProvider.etaSeconds(
                    new GeoPoint(request.fromLat(), request.fromLon()),
                    new GeoPoint(request.toLat(), request.toLon()),
                    Instant.now()
            );
            return new EtaResponse(eta, true);
        }

        double eta = primaryProvider.etaSeconds(
                new GeoPoint(request.fromLat(), request.fromLon()),
                new GeoPoint(request.toLat(), request.toLon()),
                Instant.now()
        );
        return new EtaResponse(eta, false);
    }

    public EtaResponse calculateFallback(EtaRequest request, Throwable t) {
        log.warn("Primary routing provider failed: {}. Falling back to HaversineEta.", t.getMessage());
        double eta = fallbackProvider.etaSeconds(
                new GeoPoint(request.fromLat(), request.fromLon()),
                new GeoPoint(request.toLat(), request.toLon()),
                Instant.now()
        );
        return new EtaResponse(eta, true);
    }
}
