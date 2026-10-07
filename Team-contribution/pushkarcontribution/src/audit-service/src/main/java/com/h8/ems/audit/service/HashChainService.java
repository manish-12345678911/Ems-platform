package com.h8.ems.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h8.ems.audit.dto.VerificationResult;
import com.h8.ems.audit.model.AuditLogEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class HashChainService {

    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String canonicalizePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return "{}";
        }
        try {
            JsonNode tree = objectMapper.readTree(payload);
            return objectMapper.writeValueAsString(tree);
        } catch (Exception e) {
            return payload.trim();
        }
    }

    public Instant canonicalizeAt(Instant at) {
        return (at != null ? at : Instant.now()).truncatedTo(ChronoUnit.MILLIS);
    }

    public String calculateHash(String prevHash, UUID eventId, String kind, String payload, Instant at) {
        String safePrev = (prevHash == null || prevHash.isBlank()) ? GENESIS_HASH : prevHash.trim();
        String safePayload = canonicalizePayload(payload);
        long epochMilli = canonicalizeAt(at).toEpochMilli();

        String rawContent = safePrev + ":" + eventId + ":" + kind + ":" + safePayload + ":" + epochMilli;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawContent.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public VerificationResult verifyChain(List<AuditLogEntity> entries) {
        if (entries == null || entries.isEmpty()) {
            return VerificationResult.success(0);
        }

        String expectedPrevHash = GENESIS_HASH;

        for (int i = 0; i < entries.size(); i++) {
            AuditLogEntity entry = entries.get(i);
            String actualPrev = entry.getPrevHash() == null ? GENESIS_HASH : entry.getPrevHash().trim();

            if (!Objects.equals(actualPrev, expectedPrevHash)) {
                return VerificationResult.failure(
                        entries.size(),
                        entry.getSeq(),
                        expectedPrevHash,
                        actualPrev,
                        "Chain broken at seq " + entry.getSeq() + ": prev_hash does not match previous entry's hash"
                );
            }

            String recomputedHash = calculateHash(
                    actualPrev,
                    entry.getEventId(),
                    entry.getKind(),
                    entry.getPayload(),
                    entry.getAt()
            );

            if (!Objects.equals(recomputedHash, entry.getHash().trim())) {
                return VerificationResult.failure(
                        entries.size(),
                        entry.getSeq(),
                        recomputedHash,
                        entry.getHash(),
                        "Tamper detected at seq " + entry.getSeq() + ": signature does not match recomputed SHA-256 hash"
                );
            }

            expectedPrevHash = entry.getHash().trim();
        }

        return VerificationResult.success(entries.size());
    }
}
