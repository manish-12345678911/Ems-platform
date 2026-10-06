package com.h8.ems.audit.service;

import com.h8.ems.audit.dto.VerificationResult;
import com.h8.ems.audit.model.AuditLogEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HashChainServiceTest {

    private HashChainService hashChainService;

    @BeforeEach
    void setUp() {
        hashChainService = new HashChainService();
    }

    @Test
    @DisplayName("Hash calculation is deterministic given identical inputs")
    void testDeterministicHash() {
        UUID eventId = UUID.randomUUID();
        Instant at = Instant.now();
        String payload = "{\"incidentId\":\"abc-123\",\"severity\":\"HIGH\"}";

        String hash1 = hashChainService.calculateHash(HashChainService.GENESIS_HASH, eventId, "INCIDENT_CREATED", payload, at);
        String hash2 = hashChainService.calculateHash(HashChainService.GENESIS_HASH, eventId, "INCIDENT_CREATED", payload, at);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Verify valid chain passes successfully")
    void testValidChain() {
        List<AuditLogEntity> entries = new ArrayList<>();
        String prevHash = HashChainService.GENESIS_HASH;

        for (long seq = 1; seq <= 5; seq++) {
            UUID eventId = UUID.randomUUID();
            Instant at = Instant.ofEpochMilli(1700000000000L + seq * 1000);
            String payload = "{\"step\":" + seq + "}";
            String kind = "TEST_EVENT";

            String hash = hashChainService.calculateHash(prevHash, eventId, kind, payload, at);
            AuditLogEntity entity = new AuditLogEntity(eventId, kind, "test-user", payload, prevHash, hash, at);
            entity.setSeq(seq);
            entries.add(entity);

            prevHash = hash;
        }

        VerificationResult result = hashChainService.verifyChain(entries);
        assertTrue(result.valid());
        assertEquals(5, result.totalEntries());
        assertNull(result.brokenAtSeq());
    }

    @Test
    @DisplayName("Tamper detection identifies modified payload")
    void testTamperDetectionModifiedPayload() {
        List<AuditLogEntity> entries = new ArrayList<>();
        String prevHash = HashChainService.GENESIS_HASH;

        for (long seq = 1; seq <= 3; seq++) {
            UUID eventId = UUID.randomUUID();
            Instant at = Instant.ofEpochMilli(1700000000000L + seq * 1000);
            String payload = "{\"amount\":" + seq * 100 + "}";
            String kind = "PAYMENT";

            String hash = hashChainService.calculateHash(prevHash, eventId, kind, payload, at);
            AuditLogEntity entity = new AuditLogEntity(eventId, kind, "auditor", payload, prevHash, hash, at);
            entity.setSeq(seq);
            entries.add(entity);
            prevHash = hash;
        }

        // Tamper: alter payload in entry 2 without recalculating hash
        entries.get(1).setPayload("{\"amount\":999999}");

        VerificationResult result = hashChainService.verifyChain(entries);
        assertFalse(result.valid());
        assertEquals(2L, result.brokenAtSeq());
        assertTrue(result.details().contains("Tamper detected at seq 2"));
    }

    @Test
    @DisplayName("Tamper detection identifies broken chain when intermediate block is deleted")
    void testTamperDetectionDeletedBlock() {
        List<AuditLogEntity> entries = new ArrayList<>();
        String prevHash = HashChainService.GENESIS_HASH;

        for (long seq = 1; seq <= 4; seq++) {
            UUID eventId = UUID.randomUUID();
            Instant at = Instant.ofEpochMilli(1700000000000L + seq * 1000);
            String payload = "{\"step\":" + seq + "}";
            String kind = "STEP";

            String hash = hashChainService.calculateHash(prevHash, eventId, kind, payload, at);
            AuditLogEntity entity = new AuditLogEntity(eventId, kind, "system", payload, prevHash, hash, at);
            entity.setSeq(seq);
            entries.add(entity);
            prevHash = hash;
        }

        // Remove block at index 1 (seq 2)
        entries.remove(1);

        VerificationResult result = hashChainService.verifyChain(entries);
        assertFalse(result.valid());
        assertEquals(3L, result.brokenAtSeq());
        assertTrue(result.details().contains("Chain broken at seq 3"));
    }
}
