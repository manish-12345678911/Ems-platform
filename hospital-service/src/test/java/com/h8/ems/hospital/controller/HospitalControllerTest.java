package com.h8.ems.hospital.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.h8.ems.contracts.dto.*;
import com.h8.ems.hospital.model.HospitalEntity;
import com.h8.ems.hospital.repository.HospitalRepository;
import com.h8.ems.hospital.service.AlertHub;
import com.h8.ems.hospital.service.HandoverService;
import com.h8.ems.hospital.service.HospitalCapacityService;
import com.h8.ems.hospital.service.HospitalRecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HospitalControllerTest {

    private MockMvc mockMvc;
    private HospitalRepository hospitalRepository;
    private HospitalCapacityService capacityService;
    private HospitalRecommendationService recommendationService;
    private AlertHub alertHub;
    private HandoverService handoverService;
    private ObjectMapper objectMapper;
    private final GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    void setUp() {
        hospitalRepository = mock(HospitalRepository.class);
        capacityService = mock(HospitalCapacityService.class);
        recommendationService = mock(HospitalRecommendationService.class);
        alertHub = mock(AlertHub.class);
        handoverService = mock(HandoverService.class);

        HospitalController controller = new HospitalController(
                hospitalRepository,
                capacityService,
                recommendationService,
                alertHub,
                handoverService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testGetAllHospitals() throws Exception {
        UUID hId = UUID.randomUUID();
        Point loc = gf.createPoint(new Coordinate(77.2090, 28.6139));
        HospitalEntity hospital = new HospitalEntity(hId, "City General", loc, Set.of());
        when(hospitalRepository.findAll()).thenReturn(List.of(hospital));

        mockMvc.perform(get("/hospitals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("City General"));
    }

    @Test
    void testUpdateCapacity() throws Exception {
        UUID hId = UUID.randomUUID();
        UpdateCapacityRequest req = new UpdateCapacityRequest(8, 3, 1);
        HospitalCapacityResponse res = new HospitalCapacityResponse(
                hId, "City General", 8, 3, 1, Instant.now(), false
        );

        when(capacityService.updateCapacity(eq(hId), any(UpdateCapacityRequest.class))).thenReturn(res);

        mockMvc.perform(put("/hospitals/" + hId + "/capacity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.edBedsFree").value(8))
                .andExpect(jsonPath("$.icuBedsFree").value(3));
    }

    @Test
    void testRecommendHospitals() throws Exception {
        UUID hId = UUID.randomUUID();
        HospitalRecommendationResponse rec = new HospitalRecommendationResponse(
                hId, "Trauma Center", 12.5, 300.0, 0.0, false
        );

        when(recommendationService.recommendDestinations(anyDouble(), anyDouble(), anyString(), any()))
                .thenReturn(List.of(rec));

        mockMvc.perform(get("/hospitals/recommend?lat=28.6&lon=77.2&need=TRAUMA&severity=CRITICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Trauma Center"))
                .andExpect(jsonPath("$[0].score").value(12.5));
    }

    @Test
    void testCompleteHandover() throws Exception {
        UUID hId = UUID.randomUUID();
        UUID incId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();

        HandoverRequest req = new HandoverRequest(incId, unitId, "Patient stable");
        when(handoverService.recordHandover(eq(hId), any(HandoverRequest.class)))
                .thenReturn(Map.of("status", "HANDED_OVER", "incidentId", incId.toString()));

        mockMvc.perform(post("/hospitals/" + hId + "/handover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HANDED_OVER"));
    }
}
