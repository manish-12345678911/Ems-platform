package com.h8.ems.redeployment.controller;

import com.h8.ems.contracts.dto.RedeploySuggestionDto;
import com.h8.ems.redeployment.model.RedeployMoveEntity;
import com.h8.ems.redeployment.service.RedeploymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping
public class RedeploymentController {

    private final RedeploymentService redeploymentService;

    public RedeploymentController(RedeploymentService redeploymentService) {
        this.redeploymentService = redeploymentService;
    }

    @PostMapping({"/redeployment/plan", "/redeploy/run"})
    public ResponseEntity<Map<String, Object>> triggerPlan() {
        boolean executed = redeploymentService.runPlanningWithLock();
        return ResponseEntity.ok(Map.of(
                "executed", executed,
                "message", executed ? "Redeployment planning completed" : "Lock busy, skipped run"
        ));
    }

    @GetMapping("/redeployment/suggestions")
    public ResponseEntity<List<RedeploySuggestionDto>> getSuggestions() {
        return ResponseEntity.ok(redeploymentService.getPendingSuggestions());
    }

    @PostMapping("/redeployment/accept/{id}")
    public ResponseEntity<?> acceptMove(@PathVariable("id") UUID moveId) {
        return redeploymentService.acceptMove(moveId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/redeployment/decline/{id}")
    public ResponseEntity<?> declineMove(@PathVariable("id") UUID moveId) {
        return redeploymentService.declineMove(moveId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping({"/redeployment/coverage", "/coverage"})
    public ResponseEntity<Map<String, Object>> getCoverage() {
        double coverage = redeploymentService.calculateCurrentCoverage();
        return ResponseEntity.ok(Map.of(
                "coverage", coverage,
                "coveragePercent", String.format("%.1f%%", coverage * 100)
        ));
    }
}
