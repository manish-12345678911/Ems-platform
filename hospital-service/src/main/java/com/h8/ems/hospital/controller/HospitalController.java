package com.h8.ems.hospital.controller;

import com.h8.ems.common.model.Severity;
import com.h8.ems.contracts.dto.*;
import com.h8.ems.hospital.model.HospitalEntity;
import com.h8.ems.hospital.repository.HospitalRepository;
import com.h8.ems.hospital.service.AlertHub;
import com.h8.ems.hospital.service.HandoverService;
import com.h8.ems.hospital.service.HospitalCapacityService;
import com.h8.ems.hospital.service.HospitalRecommendationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for hospital operations, capacity updates, recommendations, SSE alerts, and handover.
 */
@RestController
@RequestMapping("/hospitals")
public class HospitalController {

    private final HospitalRepository hospitalRepository;
    private final HospitalCapacityService capacityService;
    private final HospitalRecommendationService recommendationService;
    private final AlertHub alertHub;
    private final HandoverService handoverService;

    public HospitalController(
            HospitalRepository hospitalRepository,
            HospitalCapacityService capacityService,
            HospitalRecommendationService recommendationService,
            AlertHub alertHub,
            HandoverService handoverService) {
        this.hospitalRepository = hospitalRepository;
        this.capacityService = capacityService;
        this.recommendationService = recommendationService;
        this.alertHub = alertHub;
        this.handoverService = handoverService;
    }

    @GetMapping
    public ResponseEntity<List<HospitalEntity>> getAllHospitals() {
        return ResponseEntity.ok(hospitalRepository.findAll());
    }

    @PutMapping("/{id}/capacity")
    public ResponseEntity<HospitalCapacityResponse> updateCapacity(
            @PathVariable("id") UUID id,
            @RequestBody UpdateCapacityRequest request) {
        HospitalCapacityResponse response = capacityService.updateCapacity(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/capacity")
    public ResponseEntity<HospitalCapacityResponse> getCapacity(@PathVariable("id") UUID id) {
        HospitalCapacityResponse response = capacityService.getCapacity(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recommend")
    public ResponseEntity<List<HospitalRecommendationResponse>> recommendHospitals(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon,
            @RequestParam(value = "need", defaultValue = "GENERAL") String need,
            @RequestParam(value = "severity", defaultValue = "URGENT") String severityStr) {
        Severity severity;
        try {
            severity = Severity.valueOf(severityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            severity = Severity.URGENT;
        }

        List<HospitalRecommendationResponse> recommendations =
                recommendationService.recommendDestinations(lat, lon, need, severity);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping(value = "/{id}/alerts", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAlerts(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        return alertHub.subscribe(id, lastEventId);
    }

    @PostMapping("/{id}/alerts")
    public ResponseEntity<Map<String, Object>> triggerAlert(
            @PathVariable("id") UUID id,
            @RequestBody PreArrivalAlertDto alert) {
        PreArrivalAlertDto toSend = new PreArrivalAlertDto(
                alert.alertId() != null ? alert.alertId() : UUID.randomUUID(),
                alert.incidentId() != null ? alert.incidentId() : UUID.randomUUID(),
                id,
                alert.severity() != null ? alert.severity() : "URGENT",
                alert.need() != null ? alert.need() : "GENERAL",
                alert.etaSeconds(),
                alert.sentAt() != null ? alert.sentAt() : Instant.now()
        );
        alertHub.publishAlert(toSend);
        return ResponseEntity.ok(Map.of("status", "PUBLISHED", "alertId", toSend.alertId()));
    }

    @PostMapping("/{id}/handover")
    public ResponseEntity<Map<String, Object>> completeHandover(
            @PathVariable("id") UUID id,
            @RequestBody HandoverRequest request) {
        Map<String, Object> result = handoverService.recordHandover(id, request);
        return ResponseEntity.ok(result);
    }
}
