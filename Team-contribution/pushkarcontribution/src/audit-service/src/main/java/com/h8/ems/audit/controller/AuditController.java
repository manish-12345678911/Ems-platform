package com.h8.ems.audit.controller;

import com.h8.ems.audit.dto.AuditLogDto;
import com.h8.ems.audit.dto.AuditSummaryDto;
import com.h8.ems.audit.dto.CreateAuditRequest;
import com.h8.ems.audit.dto.VerificationResult;
import com.h8.ems.audit.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/audit")
    public ResponseEntity<List<AuditLogDto>> getAuditLogs(
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String incidentId,
            @RequestParam(defaultValue = "100") int limit) {
        List<AuditLogDto> results = auditService.query(kind, incidentId, limit);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/audit/verify")
    public ResponseEntity<VerificationResult> verifyChain() {
        VerificationResult result = auditService.verifyIntegrity();
        if (result.valid()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
    }

    @PostMapping("/audit")
    public ResponseEntity<AuditLogDto> recordAuditLog(@RequestBody CreateAuditRequest request) {
        AuditLogDto recorded = auditService.recordEvent(
                request.eventId(),
                request.kind(),
                request.actor(),
                request.payload(),
                request.at()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(recorded);
    }

    @GetMapping("/metrics/summary")
    public ResponseEntity<AuditSummaryDto> getMetricsSummary() {
        return ResponseEntity.ok(auditService.getSummary());
    }
}
