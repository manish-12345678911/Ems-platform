package com.h8.ems.redeployment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.h8.ems.contracts.dto.RedeploySuggestionDto;
import com.h8.ems.redeployment.model.RedeployMoveEntity;
import com.h8.ems.redeployment.service.RedeploymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RedeploymentControllerTest {

    private MockMvc mockMvc;
    private RedeploymentService redeploymentService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        redeploymentService = mock(RedeploymentService.class);
        RedeploymentController controller = new RedeploymentController(redeploymentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /redeployment/plan executes plan under lock")
    void testTriggerPlan() throws Exception {
        when(redeploymentService.runPlanningWithLock()).thenReturn(true);

        mockMvc.perform(post("/redeployment/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executed").value(true));
    }

    @Test
    @DisplayName("GET /redeployment/suggestions returns list of pending moves")
    void testGetSuggestions() throws Exception {
        UUID moveId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        RedeploySuggestionDto dto = new RedeploySuggestionDto(
                moveId, unitId, "AMB-01", "51.5074,-0.1278", 0.15, false, Instant.now()
        );
        when(redeploymentService.getPendingSuggestions()).thenReturn(List.of(dto));

        mockMvc.perform(get("/redeployment/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].moveId").value(moveId.toString()))
                .andExpect(jsonPath("$[0].coverageGain").value(0.15));
    }

    @Test
    @DisplayName("POST /redeployment/accept/{id} accepts suggestion")
    void testAcceptMove() throws Exception {
        UUID moveId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        RedeployMoveEntity move = new RedeployMoveEntity(
                moveId, unitId, "51.5074,-0.1278", 0.15, true, Instant.now()
        );
        when(redeploymentService.acceptMove(moveId)).thenReturn(Optional.of(move));

        mockMvc.perform(post("/redeployment/accept/" + moveId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true));
    }

    @Test
    @DisplayName("GET /redeployment/coverage returns coverage metrics")
    void testGetCoverage() throws Exception {
        when(redeploymentService.calculateCurrentCoverage()).thenReturn(0.85);

        mockMvc.perform(get("/redeployment/coverage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverage").value(0.85))
                .andExpect(jsonPath("$.coveragePercent").value("85.0%"));
    }
}
