package com.h8.ems.contracts.dto;

import java.util.UUID;

/**
 * REST DTO for ETA requests to routing-service.
 */
public record EtaRequest(
        double fromLat,
        double fromLon,
        double toLat,
        double toLon
) {
}
