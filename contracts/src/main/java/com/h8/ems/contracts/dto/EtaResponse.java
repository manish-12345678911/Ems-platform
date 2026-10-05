package com.h8.ems.contracts.dto;

/**
 * REST DTO for ETA responses from routing-service.
 */
public record EtaResponse(
        double etaSeconds,
        boolean fallback
) {
}
