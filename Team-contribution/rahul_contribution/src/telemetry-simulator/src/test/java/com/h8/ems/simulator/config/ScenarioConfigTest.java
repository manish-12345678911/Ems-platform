package com.h8.ems.simulator.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioConfigTest {

    @Test
    void defaultValuesAreReasonable() {
        ScenarioConfig config = new ScenarioConfig();

        assertEquals("default", config.getName());
        assertEquals(24, config.getDurationHours());
        assertEquals(20, config.getFleetSize());
        assertEquals(0.4, config.getAlsFraction(), 0.001);
        assertEquals(5.0, config.getBaseDemandPerHour(), 0.001);
        assertEquals(8.0, config.getCoverageRadiusKm(), 0.001);
        assertEquals(480, config.getResponseTargetSeconds(), 0.001);
        assertEquals(900, config.getMeanSceneTimeSeconds(), 0.001);
        assertEquals(1200, config.getMeanHandoverSeconds(), 0.001);
        assertFalse(config.getDemandProfiles().isEmpty());
        assertFalse(config.getSeverityDistribution().isEmpty());
    }

    @Test
    void settersWorkCorrectly() {
        ScenarioConfig config = new ScenarioConfig();
        config.setName("test");
        config.setDurationHours(12);
        config.setFleetSize(10);
        config.setAlsFraction(0.5);
        config.setBaseDemandPerHour(3.0);

        assertEquals("test", config.getName());
        assertEquals(12, config.getDurationHours());
        assertEquals(10, config.getFleetSize());
        assertEquals(0.5, config.getAlsFraction(), 0.001);
        assertEquals(3.0, config.getBaseDemandPerHour(), 0.001);
    }

    @Test
    void yamlDeserialization() {
        InputStream is = getClass().getResourceAsStream("/scenarios/S1.yaml");
        assertNotNull(is, "S1.yaml should be on the classpath");

        Yaml yaml = new Yaml();
        ScenarioConfig config = yaml.loadAs(is, ScenarioConfig.class);

        assertEquals("S1", config.getName());
        assertEquals(24, config.getDurationHours());
        assertEquals(20, config.getFleetSize());
        assertEquals(5.0, config.getBaseDemandPerHour(), 0.001);
        assertFalse(config.getStations().isEmpty(), "Should have stations");
        assertEquals(5, config.getStations().size());
        assertFalse(config.getHospitals().isEmpty(), "Should have hospitals");
        assertEquals(5, config.getHospitals().size());
        assertFalse(config.getZones().isEmpty(), "Should have zones");
        assertEquals(5, config.getZones().size());
    }

    @Test
    void hospitalConfigToClinicalNeeds() {
        ScenarioConfig.HospitalConfig hc = new ScenarioConfig.HospitalConfig();
        hc.setCapabilities(java.util.List.of("TRAUMA", "CARDIAC", "GENERAL"));

        var needs = hc.toClinicalNeeds();
        assertEquals(3, needs.size());
        assertTrue(needs.contains(com.h8.ems.common.model.ClinicalNeed.TRAUMA));
        assertTrue(needs.contains(com.h8.ems.common.model.ClinicalNeed.CARDIAC));
        assertTrue(needs.contains(com.h8.ems.common.model.ClinicalNeed.GENERAL));
    }

    @Test
    void zoneConfigToGeoPoint() {
        ScenarioConfig.ZoneConfig zc = new ScenarioConfig.ZoneConfig(51.50, -0.12, 2.0);
        var gp = zc.toGeoPoint();
        assertEquals(51.50, gp.lat(), 0.001);
        assertEquals(-0.12, gp.lon(), 0.001);
    }

    @Test
    void stationConfigToGeoPoint() {
        ScenarioConfig.StationConfig sc = new ScenarioConfig.StationConfig("Central", 51.50, -0.12, 5);
        var gp = sc.toGeoPoint();
        assertEquals(51.50, gp.lat(), 0.001);
        assertEquals(-0.12, gp.lon(), 0.001);
        assertEquals("Central", sc.getName());
        assertEquals(5, sc.getUnits());
    }
}
