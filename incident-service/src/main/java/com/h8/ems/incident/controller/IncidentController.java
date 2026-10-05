package com.h8.ems.incident.controller;

import com.h8.ems.contracts.dto.CreateIncidentRequest;
import com.h8.ems.incident.model.IncidentEntity;
import com.h8.ems.incident.service.IncidentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for incident operations.
 */
@RestController
@RequestMapping("/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createIncident(@RequestBody CreateIncidentRequest request) {
        IncidentEntity created = incidentService.createIncident(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "incidentId", created.getId(),
                "status", created.getStatus().name(),
                "receivedAt", created.getReceivedAt().toString()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentEntity> getIncident(@PathVariable("id") UUID id) {
        return incidentService.getIncident(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
