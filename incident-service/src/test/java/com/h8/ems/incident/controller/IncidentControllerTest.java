package com.h8.ems.incident.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h8.ems.common.model.ClinicalNeed;
import com.h8.ems.common.model.IncidentStatus;
import com.h8.ems.common.model.Severity;
import com.h8.ems.contracts.dto.CreateIncidentRequest;
import com.h8.ems.incident.config.SecurityConfig;
import com.h8.ems.incident.model.IncidentEntity;
import com.h8.ems.incident.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IncidentController.class)
@Import(SecurityConfig.class)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IncidentService incidentService;

    @Test
    void createIncidentReturns201Created() throws Exception {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        IncidentEntity entity = new IncidentEntity();
        entity.setId(id);
        entity.setStatus(IncidentStatus.RECEIVED);
        entity.setReceivedAt(now);
        entity.setSeverity(Severity.CRITICAL);
        entity.setNeed(ClinicalNeed.CARDIAC);

        when(incidentService.createIncident(any(CreateIncidentRequest.class))).thenReturn(entity);

        CreateIncidentRequest req = new CreateIncidentRequest(
                51.50, -0.12, "CRITICAL", "CARDIAC", true, "+442079460000"
        );

        mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentId").value(id.toString()))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void getIncidentReturns200WhenFound() throws Exception {
        UUID id = UUID.randomUUID();
        IncidentEntity entity = new IncidentEntity();
        entity.setId(id);
        entity.setStatus(IncidentStatus.RECEIVED);
        entity.setReceivedAt(Instant.now());
        entity.setSeverity(Severity.EMERGENCY);
        entity.setNeed(ClinicalNeed.TRAUMA);

        when(incidentService.getIncident(id)).thenReturn(Optional.of(entity));

        mockMvc.perform(get("/incidents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void getIncidentReturns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(incidentService.getIncident(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/incidents/{id}", id))
                .andExpect(status().isNotFound());
    }
}
