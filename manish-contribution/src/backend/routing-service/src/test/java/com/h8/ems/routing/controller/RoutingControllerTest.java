package com.h8.ems.routing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h8.ems.contracts.dto.EtaRequest;
import com.h8.ems.contracts.dto.EtaResponse;
import com.h8.ems.routing.config.SecurityConfig;
import com.h8.ems.routing.service.RoutingEtaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoutingController.class)
@Import(SecurityConfig.class)
class RoutingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoutingEtaService etaService;

    @Test
    void postEtaReturnsEtaResponse() throws Exception {
        when(etaService.calculateEta(any())).thenReturn(new EtaResponse(180.0, true));

        EtaRequest req = new EtaRequest(51.50, -0.12, 51.52, -0.08);

        mockMvc.perform(post("/eta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etaSeconds").value(180.0))
                .andExpect(jsonPath("$.fallback").value(true));
    }

    @Test
    void getEtaReturnsEtaResponse() throws Exception {
        when(etaService.calculateEta(any())).thenReturn(new EtaResponse(240.0, false));

        mockMvc.perform(get("/eta")
                        .param("fromLat", "51.50")
                        .param("fromLon", "-0.12")
                        .param("toLat", "51.52")
                        .param("toLon", "-0.08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etaSeconds").value(240.0))
                .andExpect(jsonPath("$.fallback").value(false));
    }
}
