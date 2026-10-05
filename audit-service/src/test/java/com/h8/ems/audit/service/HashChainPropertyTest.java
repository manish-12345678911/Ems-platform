package com.h8.ems.audit.service;

import com.h8.ems.audit.dto.VerificationResult;
import com.h8.ems.audit.model.AuditLogEntity;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotEmpty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class HashChainPropertyTest {

    private final HashChainService hashChainService = new HashChainService();

    @Property
    void validChainsAlwaysVerifySuccessfully(
            @ForAll @IntRange(min = 1, max = 25) int length,
            @ForAll("validPayloads") List<String> payloads) {

        List<AuditLogEntity> entries = new ArrayList<>();
        String prevHash = HashChainService.GENESIS_HASH;

        for (int i = 0; i < length; i++) {
            UUID eventId = UUID.randomUUID();
            Instant at = Instant.ofEpochMilli(1700000000000L + (i * 1000L));
            String payload = payloads.get(i % payloads.size());
            String kind = "TEST_KIND_" + (i % 5);

            String hash = hashChainService.calculateHash(prevHash, eventId, kind, payload, at);
            AuditLogEntity entity = new AuditLogEntity(eventId, kind, "user", payload, prevHash, hash, at);
            entity.setSeq((long) (i + 1));
            entries.add(entity);

            prevHash = hash;
        }

        VerificationResult result = hashChainService.verifyChain(entries);
        Assume.that(result != null);
        assert result.valid();
        assert result.totalEntries() == length;
    }

    @Property
    void corruptedPayloadAlwaysFailsVerification(
            @ForAll @IntRange(min = 2, max = 20) int length,
            @ForAll("validPayloads") List<String> payloads,
            @ForAll @IntRange(min = 0, max = 19) int tamperIndex) {

        int safeTamperIndex = tamperIndex % length;
        List<AuditLogEntity> entries = new ArrayList<>();
        String prevHash = HashChainService.GENESIS_HASH;

        for (int i = 0; i < length; i++) {
            UUID eventId = UUID.randomUUID();
            Instant at = Instant.ofEpochMilli(1700000000000L + (i * 1000L));
            String payload = payloads.get(i % payloads.size());
            String kind = "EVENT";

            String hash = hashChainService.calculateHash(prevHash, eventId, kind, payload, at);
            AuditLogEntity entity = new AuditLogEntity(eventId, kind, "user", payload, prevHash, hash, at);
            entity.setSeq((long) (i + 1));
            entries.add(entity);

            prevHash = hash;
        }

        // Inject tampering
        AuditLogEntity tampered = entries.get(safeTamperIndex);
        tampered.setPayload(tampered.getPayload() + "_CORRUPTED_EXTRA_DATA");

        VerificationResult result = hashChainService.verifyChain(entries);
        assert !result.valid();
        assert result.brokenAtSeq() != null;
    }

    @Provide
    Arbitrary<List<String>> validPayloads() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50).list().ofMinSize(1).ofMaxSize(30);
    }
}
