package com.h8.ems.dispatch.service;

import com.h8.ems.common.model.*;
import com.h8.ems.common.scoring.CoverageModel;
import com.h8.ems.common.scoring.DispatchScorer;
import com.h8.ems.contracts.dto.CandidateRankingResponse;
import com.h8.ems.contracts.dto.NearbyUnitResponse;
import com.h8.ems.dispatch.client.RoutingClient;
import com.h8.ems.dispatch.client.TrackingClient;
import com.h8.ems.dispatch.model.AmbulanceUnitEntity;
import com.h8.ems.dispatch.model.StationEntity;
import com.h8.ems.dispatch.repository.AmbulanceUnitRepository;
import com.h8.ems.dispatch.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that ranks candidate ambulance units for an incident using DispatchScorer from common (Hard Rule #2).
 * Implements adaptive radius widening for rural scenarios (Correction #5).
 */
@Service
public class DispatchRankingService {

    private static final Logger log = LoggerFactory.getLogger(DispatchRankingService.class);

    private final AmbulanceUnitRepository unitRepository;
    private final StationRepository stationRepository;
    private final TrackingClient trackingClient;
    private final RoutingClient routingClient;

    private final double initialRadiusKm;
    private final int maxCandidates;
    private final double ruralFallbackRadiusKm;
    private final double maxRadiusKm;

    public DispatchRankingService(
            AmbulanceUnitRepository unitRepository,
            StationRepository stationRepository,
            TrackingClient trackingClient,
            RoutingClient routingClient,
            @Value("${h8.dispatch.nearby-radius-km:25.0}") double initialRadiusKm,
            @Value("${h8.dispatch.max-candidates:15}") int maxCandidates,
            @Value("${h8.dispatch.rural-fallback-radius-km:50.0}") double ruralFallbackRadiusKm,
            @Value("${h8.dispatch.max-radius-km:100.0}") double maxRadiusKm) {
        this.unitRepository = unitRepository;
        this.stationRepository = stationRepository;
        this.trackingClient = trackingClient;
        this.routingClient = routingClient;
        this.initialRadiusKm = initialRadiusKm;
        this.maxCandidates = maxCandidates;
        this.ruralFallbackRadiusKm = ruralFallbackRadiusKm;
        this.maxRadiusKm = maxRadiusKm;
    }

    /**
     * Ranks available candidate units for an incident.
     */
    @Transactional(readOnly = true)
    public List<CandidateRankingResponse> rankCandidates(IncidentSnapshot incident, Instant now) {
        if (now == null) {
            now = Instant.now();
        }

        GeoPoint incidentLoc = incident.location();
        if (incidentLoc == null) {
            return List.of();
        }

        // 1. Query nearby available units, widening radius adaptively if needed (Correction #5)
        List<NearbyUnitResponse> nearby = findNearbyWithAdaptiveWidening(incidentLoc.lat(), incidentLoc.lon());
        if (nearby.isEmpty()) {
            log.info("No nearby available units found for incident {} even after adaptive widening", incident.id());
            return List.of();
        }

        Map<UUID, Double> distanceMap = nearby.stream()
                .collect(Collectors.toMap(NearbyUnitResponse::unitId, NearbyUnitResponse::distanceKm, (a, b) -> a));

        List<UUID> candidateIds = nearby.stream().map(NearbyUnitResponse::unitId).toList();
        List<AmbulanceUnitEntity> candidateEntities = unitRepository.findByIdInAndStatus(candidateIds, UnitStatus.AVAILABLE);

        if (candidateEntities.isEmpty()) {
            return List.of();
        }

        // 2. Prepare coverage model and snapshots
        List<AmbulanceUnitEntity> allAvailableEntities = unitRepository.findByStatus(UnitStatus.AVAILABLE);
        List<UnitSnapshot> allAvailableSnapshots = allAvailableEntities.stream()
                .map(AmbulanceUnitEntity::toSnapshot)
                .toList();

        List<GeoPoint> demandZones = stationRepository.findAll().stream()
                .filter(s -> s.getLocation() != null)
                .map(s -> new GeoPoint(s.getLocation().getY(), s.getLocation().getX()))
                .toList();

        CoverageModel coverageModel = new CoverageModel(initialRadiusKm, demandZones);
        DispatchScorer scorer = new DispatchScorer(coverageModel);

        // 3. Score each candidate unit
        List<CandidateRankingResponse> ranked = new ArrayList<>();
        for (AmbulanceUnitEntity entity : candidateEntities) {
            UnitSnapshot unitSnapshot = entity.toSnapshot();
            GeoPoint unitPos = unitSnapshot.position();

            double etaSeconds = routingClient.getEtaSeconds(unitPos, incidentLoc, now);
            DispatchScorer.ScoreBreakdown breakdown = scorer.breakdown(
                    unitSnapshot, incident, etaSeconds, now, allAvailableSnapshots
            );

            double distKm = distanceMap.getOrDefault(entity.getId(), unitPos != null ? unitPos.distanceTo(incidentLoc) : 0.0);

            ranked.add(new CandidateRankingResponse(
                    entity.getId(),
                    entity.getCallSign(),
                    entity.getType().name(),
                    breakdown.totalScore(),
                    breakdown.etaSeconds(),
                    distKm,
                    breakdown.etaComponent(),
                    breakdown.capabilityComponent(),
                    breakdown.fatigueComponent(),
                    breakdown.coverageComponent(),
                    breakdown.stalenessComponent()
            ));
        }

        // 4. Sort ascending (lower score = better match)
        ranked.sort(Comparator.comparingDouble(CandidateRankingResponse::score));

        return ranked;
    }

    /**
     * Adaptive radius widening for rural or low-density scenarios (Correction #5).
     */
    private List<NearbyUnitResponse> findNearbyWithAdaptiveWidening(double lat, double lon) {
        double currentRadius = initialRadiusKm;
        List<NearbyUnitResponse> results = trackingClient.findNearbyUnits(lat, lon, currentRadius, maxCandidates);

        if (!results.isEmpty()) {
            return results;
        }

        // Try rural fallback radius
        if (ruralFallbackRadiusKm > currentRadius) {
            log.info("Zero candidates within {} km, widening to rural fallback radius {} km", currentRadius, ruralFallbackRadiusKm);
            results = trackingClient.findNearbyUnits(lat, lon, ruralFallbackRadiusKm, maxCandidates);
            if (!results.isEmpty()) {
                return results;
            }
        }

        // Try max radius if still empty
        if (maxRadiusKm > ruralFallbackRadiusKm) {
            log.info("Zero candidates within {} km, widening to maximum radius {} km", ruralFallbackRadiusKm, maxRadiusKm);
            results = trackingClient.findNearbyUnits(lat, lon, maxRadiusKm, maxCandidates);
        }

        return results;
    }
}
