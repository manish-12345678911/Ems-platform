package com.h8.ems.hospital.service;

import com.h8.ems.common.eta.HaversineEta;
import com.h8.ems.common.model.*;
import com.h8.ems.common.scoring.DestinationRanker;
import com.h8.ems.contracts.dto.HospitalCapacityResponse;
import com.h8.ems.contracts.dto.HospitalRecommendationResponse;
import com.h8.ems.hospital.model.HospitalEntity;
import com.h8.ems.hospital.repository.HospitalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

/**
 * Service providing destination hospital recommendations using DestinationRanker from common.
 * Complies with Rule #2 (DestinationRanker lives only in common).
 */
@Service
public class HospitalRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(HospitalRecommendationService.class);

    private final HospitalRepository hospitalRepository;
    private final HospitalCapacityService capacityService;
    private final DestinationRanker ranker;
    private final HaversineEta fallbackEta;
    private final RestTemplate restTemplate;
    private final String routingServiceUrl;

    public HospitalRecommendationService(
            HospitalRepository hospitalRepository,
            HospitalCapacityService capacityService,
            @Value("${h8.routing.url:http://localhost:8084}") String routingServiceUrl) {
        this.hospitalRepository = hospitalRepository;
        this.capacityService = capacityService;
        this.ranker = new DestinationRanker();
        this.fallbackEta = new HaversineEta();
        this.restTemplate = new RestTemplate();
        this.routingServiceUrl = routingServiceUrl;
    }

    public List<HospitalRecommendationResponse> recommendDestinations(
            double incidentLat,
            double incidentLon,
            String needStr,
            Severity severity) {

        ClinicalNeed need;
        try {
            need = ClinicalNeed.valueOf(needStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            need = ClinicalNeed.GENERAL;
        }

        GeoPoint incidentLoc = new GeoPoint(incidentLat, incidentLon);
        IncidentSnapshot incident = new IncidentSnapshot(
                UUID.randomUUID(),
                incidentLoc,
                severity != null ? severity : Severity.URGENT,
                need,
                false,
                IncidentStatus.ON_SCENE,
                Instant.now()
        );

        List<HospitalEntity> entities = hospitalRepository.findAll();
        if (entities.isEmpty()) {
            return List.of();
        }

        Instant now = Instant.now();
        int ttlMinutes = capacityService.getCapacityTtlMinutes();

        // Prepare HospitalSnapshots with live/cached capacity
        List<HospitalSnapshot> snapshots = new ArrayList<>();
        Map<UUID, HospitalEntity> entityMap = new HashMap<>();

        for (HospitalEntity entity : entities) {
            entityMap.put(entity.getId(), entity);
            HospitalCapacityResponse cap = capacityService.getCapacity(entity.getId());
            snapshots.add(entity.toSnapshot(
                    cap.edBedsFree(),
                    cap.icuBedsFree(),
                    cap.ventilatorsFree(),
                    cap.updatedAt()
            ));
        }

        // Rank hospitals using DestinationRanker from common
        List<DestinationRanker.RankedHospital> ranked = ranker.rank(
                incident,
                snapshots,
                (from, to) -> calculateEta(from, to),
                now,
                ttlMinutes
        );

        List<HospitalRecommendationResponse> responseList = new ArrayList<>();
        for (DestinationRanker.RankedHospital rh : ranked) {
            HospitalSnapshot hs = rh.hospital();
            responseList.add(new HospitalRecommendationResponse(
                    hs.id(),
                    hs.name(),
                    rh.score(),
                    rh.transportEtaSeconds(),
                    rh.estimatedWaitMinutes(),
                    rh.capacityStale()
            ));
        }

        return responseList;
    }

    private double calculateEta(GeoPoint from, GeoPoint to) {
        if (from == null || to == null) return 0.0;
        try {
            String url = String.format("%s/eta?fromLat=%f&fromLon=%f&toLat=%f&toLon=%f",
                    routingServiceUrl, from.lat(), from.lon(), to.lat(), to.lon());
            Map<?, ?> res = restTemplate.getForObject(url, Map.class);
            if (res != null && res.containsKey("etaSeconds")) {
                return ((Number) res.get("etaSeconds")).doubleValue();
            }
        } catch (Exception e) {
            log.debug("Routing service unavailable, using HaversineEta fallback: {}", e.getMessage());
        }
        return fallbackEta.etaSeconds(from, to, Instant.now());
    }
}
