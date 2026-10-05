package com.h8.ems.audit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.h8.ems.audit.dto.AuditLogDto;
import com.h8.ems.audit.dto.AuditSummaryDto;
import com.h8.ems.audit.dto.CreateAuditRequest;
import com.h8.ems.audit.dto.VerificationResult;
import com.h8.ems.audit.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditControllerTest {

    private MockMvc mockMvc;
    private AuditService auditService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        AuditController controller = new AuditController(auditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testGetAuditLogs() throws Exception {
        UUID eventId = UUID.randomUUID();
        AuditLogDto dto = new AuditLogDto(
                1L, eventId, "INCIDENT_CREATED", "dispatcher1",
                "{\"priority\":1}", "0000000000000000000000000000000000000000000000000000000000000000",
                "abcdef123456", Instant.now()
        );
        when(auditService.query(any(), any(), anyInt())).thenReturn(List.of(dto));

        mockMvc.perform(get("/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seq").value(1))
                .andExpect(jsonPath("$[0].kind").value("INCIDENT_CREATED"))
                .andExpect(jsonPath("$[0].actor").value("dispatcher1"));
    }

    @Test
    void testVerifyChainSuccess() throws Exception {
        VerificationResult result = VerificationResult.success(10);
        when(auditService.verifyIntegrity()).thenReturn(result);

        mockMvc.perform(get("/audit/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.totalEntries").value(10));
    }

    @Test
    void testVerifyChainTampered() throws Exception {
        VerificationResult result = VerificationResult.failure(5, 3L, "expected", "actual", "tamper detected");
        when(auditService.verifyIntegrity()).thenReturn(result);

        mockMvc.perform(get("/audit/verify"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.brokenAtSeq").value(3));
    }

    @Test
    void testRecordAuditLog() throws Exception {
        UUID eventId = UUID.randomUUID();
        CreateAuditRequest request = new CreateAuditRequest(
                eventId, "DISPATCH_OVERRIDE", "supervisor1", "{\"reason\":\"Closer unit\"}", Instant.now()
        );

        AuditLogDto dto = new AuditLogDto(
                2L, eventId, "DISPATCH_OVERRIDE", "supervisor1",
                "{\"reason\":\"Closer unit\"}", "prevhash", "newhash", Instant.now()
        );
        when(auditService.recordEvent(any(), any(), any(), any(), any())).thenReturn(dto);

        mockMvc.perform(post("/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seq").value(2))
                .andExpect(jsonPath("$.kind").value("DISPATCH_OVERRIDE"));
    }

    @Test
    void testGetMetricsSummary() throws Exception {
        AuditSummaryDto summary = new AuditSummaryDto(
                42, true, Instant.now(), Map.of("INCIDENT_CREATED", 20L, "DISPATCH_ASSIGNED", 22L),
                VerificationResult.success(42)
        );
        when(auditService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/metrics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(42))
                .andExpect(jsonPath("$.chainValid").value(true));
    }
}
