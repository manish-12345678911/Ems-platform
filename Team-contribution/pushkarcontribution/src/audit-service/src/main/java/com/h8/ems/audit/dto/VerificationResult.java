package com.h8.ems.audit.dto;

import java.time.Instant;

public record VerificationResult(
        boolean valid,
        long totalEntries,
        Long brokenAtSeq,
        String expectedHash,
        String actualHash,
        String details,
        Instant verifiedAt
) {
    public static VerificationResult success(long totalEntries) {
        return new VerificationResult(true, totalEntries, null, null, null, "All hash-chain signatures verified successfully", Instant.now());
    }

    public static VerificationResult failure(long totalEntries, Long brokenAtSeq, String expectedHash, String actualHash, String details) {
        return new VerificationResult(false, totalEntries, brokenAtSeq, expectedHash, actualHash, details, Instant.now());
    }
}
