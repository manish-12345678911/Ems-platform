package com.h8.ems.routing.controller;

import com.h8.ems.contracts.dto.EtaRequest;
import com.h8.ems.contracts.dto.EtaResponse;
import com.h8.ems.routing.service.RoutingEtaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing ETA calculations.
 */
@RestController
@RequestMapping("/eta")
public class RoutingController {

    private final RoutingEtaService etaService;

    public RoutingController(RoutingEtaService etaService) {
        this.etaService = etaService;
    }

    @PostMapping
    public ResponseEntity<EtaResponse> getEtaPost(@RequestBody EtaRequest request) {
        return ResponseEntity.ok(etaService.calculateEta(request));
    }

    @GetMapping
    public ResponseEntity<EtaResponse> getEtaGet(
            @RequestParam("fromLat") double fromLat,
            @RequestParam("fromLon") double fromLon,
            @RequestParam("toLat") double toLat,
            @RequestParam("toLon") double toLon) {
        return ResponseEntity.ok(etaService.calculateEta(new EtaRequest(fromLat, fromLon, toLat, toLon)));
    }
}
