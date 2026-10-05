package com.h8.ems.tracking.controller;

import com.h8.ems.contracts.dto.NearbyUnitResponse;
import com.h8.ems.tracking.config.SecurityConfig;
import com.h8.ems.tracking.service.TrackingRedisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrackingController.class)
@Import(SecurityConfig.class)
class TrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrackingRedisService trackingRedisService;

    @Test
    void getNearbyUnitsReturnsList() throws Exception {
        UUID unitId = UUID.randomUUID();
        when(trackingRedisService.findNearbyUnits(51.50, -0.12, 25.0, 15))
                .thenReturn(List.of(new NearbyUnitResponse(unitId, 2.5, 51.51, -0.11, 1700000000000L)));

        mockMvc.perform(get("/tracking/nearby")
                        .param("lat", "51.50")
                        .param("lon", "-0.12")
                        .param("radiusKm", "25.0")
                        .param("limit", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].unitId").value(unitId.toString()))
                .andExpect(jsonPath("$[0].distanceKm").value(2.5))
                .andExpect(jsonPath("$[0].lat").value(51.51))
                .andExpect(jsonPath("$[0].lon").value(-0.11));
    }
}
