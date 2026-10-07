package com.h8.ems.dispatch.controller;

import com.h8.ems.common.model.*;
import com.h8.ems.contracts.dto.CandidateRankingResponse;
import com.h8.ems.contracts.dto.DispatchRequest;
import com.h8.ems.contracts.dto.DispatchResponse;
import com.h8.ems.contracts.dto.RejectRequest;
import com.h8.ems.dispatch.service.DispatchExecutionService;
import com.h8.ems.dispatch.service.DispatchRankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for dispatch candidate ranking, dispatch confirmation, override, and crew rejection.
 */
@RestController
@RequestMapping("/dispatch")
public class DispatchController {

    private final DispatchRankingService rankingService;
    private final DispatchExecutionService executionService;
    private final com.h8.ems.dispatch.repository.AmbulanceUnitRepository unitRepository;

    public DispatchController(DispatchRankingService rankingService,
                              DispatchExecutionService executionService,
                              com.h8.ems.dispatch.repository.AmbulanceUnitRepository unitRepository) {
        this.rankingService = rankingService;
        this.executionService = executionService;
        this.unitRepository = unitRepository;
    }

    /**
     * Ranks candidate units for an incident using DispatchScorer from common.
     */
    @GetMapping("/candidates")
    public ResponseEntity<List<CandidateRankingResponse>> getCandidates(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon,
            @RequestParam(value = "incidentId", required = false) UUID incidentId,
            @RequestParam(value = "severity", defaultValue = "URGENT") String severityStr,
            @RequestParam(value = "need", defaultValue = "TRAUMA") String needStr,
            @RequestParam(value = "requiresAls", defaultValue = "false") boolean requiresAls) {

        Severity severity;
        try {
            severity = Severity.valueOf(severityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            severity = Severity.URGENT;
        }

        ClinicalNeed need;
        try {
            need = ClinicalNeed.valueOf(needStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            need = ClinicalNeed.TRAUMA;
        }

        IncidentSnapshot incident = new IncidentSnapshot(
                incidentId != null ? incidentId : UUID.randomUUID(),
                new GeoPoint(lat, lon),
                severity,
                need,
                requiresAls,
                IncidentStatus.RECEIVED,
                Instant.now()
        );

        List<CandidateRankingResponse> candidates = rankingService.rankCandidates(incident, Instant.now());
        return ResponseEntity.ok(candidates);
    }

    /**
     * Confirms unit dispatch to an incident.
     */
    @PostMapping
    public ResponseEntity<DispatchResponse> dispatchUnit(@RequestBody DispatchRequest request) {
        DispatchResponse response = executionService.dispatch(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Dispatcher override with reason.
     */
    @PostMapping("/override")
    public ResponseEntity<DispatchResponse> overrideUnit(@RequestBody DispatchRequest request) {
        DispatchResponse response = executionService.override(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Crew rejects dispatch assignment.
     */
    @PostMapping("/reject")
    public ResponseEntity<Map<String, String>> rejectDispatch(@RequestBody RejectRequest request) {
        executionService.reject(request);
        return ResponseEntity.ok(Map.of("status", "REJECTED"));
    }

    /**
     * Retrieves all ambulance units with current coordinates and status.
     */
    @GetMapping("/units")
    public ResponseEntity<List<com.h8.ems.dispatch.model.AmbulanceUnitEntity>> getAllUnits() {
        return ResponseEntity.ok(unitRepository.findAll());
    }

    public record UnitLocationUpdateRequest(Double lat, Double lon, String status) {}

    /**
     * Ingests real-time live GPS telemetry and optional status from crew mobile devices.
     * Accepts coordinates via JSON body or query parameters for maximum interoperability.
     */
    @PostMapping("/units/{unitId}/location")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String, Object>> updateUnitLocation(
            @PathVariable("unitId") UUID unitId,
            @RequestBody(required = false) UnitLocationUpdateRequest req,
            @RequestParam(value = "lat", required = false) Double queryLat,
            @RequestParam(value = "lon", required = false) Double queryLon,
            @RequestParam(value = "status", required = false) String queryStatus) {
        double finalLat = (req != null && req.lat() != null) ? req.lat() : (queryLat != null ? queryLat : 0.0);
        double finalLon = (req != null && req.lon() != null) ? req.lon() : (queryLon != null ? queryLon : 0.0);
        String finalStatus = (req != null && req.status() != null) ? req.status() : queryStatus;

        int updated = unitRepository.updatePosition(unitId, finalLat, finalLon, Instant.now());
        if (finalStatus != null && !finalStatus.isBlank()) {
            try {
                UnitStatus s = UnitStatus.valueOf(finalStatus.toUpperCase());
                unitRepository.updateUnitStatus(unitId, s);
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(Map.of(
                "updated", updated > 0,
                "unitId", unitId.toString(),
                "lat", finalLat,
                "lon", finalLon,
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Explicit status update from crew field PWA (e.g. ON_SCENE, TRANSPORTING, AT_HOSPITAL, AVAILABLE).
     */
    @PostMapping("/units/{unitId}/status")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String, Object>> updateUnitStatus(
            @PathVariable("unitId") UUID unitId,
            @RequestParam("status") String statusStr) {
        try {
            UnitStatus status = UnitStatus.valueOf(statusStr.toUpperCase());
            int updated = unitRepository.updateUnitStatus(unitId, status);
            return ResponseEntity.ok(Map.of(
                    "updated", updated > 0,
                    "unitId", unitId.toString(),
                    "status", status.name()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + statusStr));
        }
    }
}
