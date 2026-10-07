package com.h8.ems.dispatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h8.ems.common.model.*;
import com.h8.ems.contracts.dto.CandidateRankingResponse;
import com.h8.ems.contracts.dto.DispatchRequest;
import com.h8.ems.contracts.dto.DispatchResponse;
import com.h8.ems.contracts.dto.RejectRequest;
import com.h8.ems.dispatch.config.SecurityConfig;
import com.h8.ems.dispatch.service.DispatchExecutionService;
import com.h8.ems.dispatch.service.DispatchRankingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DispatchController.class)
@Import(SecurityConfig.class)
class DispatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DispatchRankingService rankingService;

    @MockBean
    private DispatchExecutionService executionService;

    @MockBean
    private com.h8.ems.dispatch.repository.AmbulanceUnitRepository unitRepository;

    @Test
    void getCandidatesReturnsOk() throws Exception {
        UUID unitId = UUID.randomUUID();
        CandidateRankingResponse candidate = new CandidateRankingResponse(
                unitId, "ALS-1", "ALS", 0.35, 300.0, 5.2,
                0.20, 0.0, 0.05, 0.08, 0.02
        );
        when(rankingService.rankCandidates(any(IncidentSnapshot.class), any(Instant.class)))
                .thenReturn(List.of(candidate));

        mockMvc.perform(get("/dispatch/candidates")
                        .param("lat", "51.50")
                        .param("lon", "-0.12")
                        .param("severity", "EMERGENCY")
                        .param("requiresAls", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].unitId").value(unitId.toString()))
                .andExpect(jsonPath("$[0].callSign").value("ALS-1"))
                .andExpect(jsonPath("$[0].score").value(0.35));
    }

    @Test
    void dispatchUnitReturnsOk() throws Exception {
        UUID incidentId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        Instant now = Instant.now();

        DispatchResponse resp = new DispatchResponse(assignmentId, incidentId, unitId, "DISPATCHED", now);
        when(executionService.dispatch(any(DispatchRequest.class))).thenReturn(resp);

        DispatchRequest req = new DispatchRequest(incidentId, unitId, "AUTO");

        mockMvc.perform(post("/dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentId").value(assignmentId.toString()))
                .andExpect(jsonPath("$.status").value("DISPATCHED"));
    }

    @Test
    void rejectDispatchReturnsOk() throws Exception {
        UUID incidentId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();

        doNothing().when(executionService).reject(any(RejectRequest.class));

        RejectRequest req = new RejectRequest(incidentId, unitId, "Mechanical failure");

        mockMvc.perform(post("/dispatch/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void updateUnitLocationReturnsOk() throws Exception {
        UUID unitId = UUID.randomUUID();
        when(unitRepository.updatePosition(eq(unitId), anyDouble(), anyDouble(), any(Instant.class))).thenReturn(1);

        mockMvc.perform(post("/dispatch/units/" + unitId + "/location")
                        .param("lat", "26.9124")
                        .param("lon", "75.7873")
                        .param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(true))
                .andExpect(jsonPath("$.unitId").value(unitId.toString()));
    }

    @Test
    void updateUnitStatusReturnsOk() throws Exception {
        UUID unitId = UUID.randomUUID();
        when(unitRepository.updateUnitStatus(eq(unitId), eq(UnitStatus.AVAILABLE))).thenReturn(1);

        mockMvc.perform(post("/dispatch/units/" + unitId + "/status")
                        .param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(true))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }
}
