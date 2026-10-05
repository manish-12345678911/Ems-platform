package com.h8.ems.tracking.controller;

import com.h8.ems.contracts.dto.NearbyUnitResponse;
import com.h8.ems.tracking.service.TrackingRedisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller providing geospatial nearby unit queries.
 */
@RestController
@RequestMapping("/tracking")
public class TrackingController {

    private final TrackingRedisService trackingRedisService;

    public TrackingController(TrackingRedisService trackingRedisService) {
        this.trackingRedisService = trackingRedisService;
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyUnitResponse>> getNearbyUnits(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon,
            @RequestParam(name = "radiusKm", defaultValue = "25.0") double radiusKm,
            @RequestParam(name = "limit", defaultValue = "15") int limit) {
        List<NearbyUnitResponse> nearby = trackingRedisService.findNearbyUnits(lat, lon, radiusKm, limit);
        return ResponseEntity.ok(nearby);
    }
}
