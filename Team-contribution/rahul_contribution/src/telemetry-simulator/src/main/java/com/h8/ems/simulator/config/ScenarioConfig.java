package com.h8.ems.simulator.config;

import com.h8.ems.common.model.ClinicalNeed;
import com.h8.ems.common.model.GeoPoint;

import java.util.*;

/**
 * Scenario configuration loaded from YAML.
 * Defines fleet, demand, hospitals, zone grid, and simulation parameters.
 */
public class ScenarioConfig {

    private String name = "default";
    private int durationHours = 24;

    // Fleet
    private int fleetSize = 20;
    private double alsFraction = 0.4;

    // Demand
    private double baseDemandPerHour = 5.0;
    private List<DemandProfile> demandProfiles = List.of(
            new DemandProfile(0, 6, 0.3),
            new DemandProfile(6, 9, 1.2),
            new DemandProfile(9, 16, 1.0),
            new DemandProfile(16, 20, 1.3),
            new DemandProfile(20, 24, 0.5)
    );
    private Map<String, Double> severityDistribution = Map.of(
            "CRITICAL", 0.05,
            "EMERGENCY", 0.25,
            "URGENT", 0.45,
            "LOW", 0.25
    );
    private double alsRequiredFraction = 0.15;

    // Geography — bounding box
    private double minLat = 51.45;
    private double maxLat = 51.55;
    private double minLon = -0.20;
    private double maxLon = -0.05;

    // Zones
    private List<ZoneConfig> zones = new ArrayList<>();

    // Stations
    private List<StationConfig> stations = new ArrayList<>();

    // Hospitals
    private List<HospitalConfig> hospitals = new ArrayList<>();

    // Scoring
    private double coverageRadiusKm = 8.0;

    // Times (seconds)
    private double meanSceneTimeSeconds = 900;      // 15 min
    private double sceneTimeStdDev = 300;            // 5 min
    private double meanHandoverSeconds = 1200;       // 20 min
    private double handoverStdDev = 600;             // 10 min
    private double travelNoiseStdDev = 0.2;          // log-normal σ

    // Response target
    private double responseTargetSeconds = 480;      // 8 min

    // Redeployment
    private int maxRedeployMoves = 3;
    private double minRedeployGain = 0.01;
    private int redeployCooldownMinutes = 10;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDurationHours() { return durationHours; }
    public void setDurationHours(int durationHours) { this.durationHours = durationHours; }

    public int getFleetSize() { return fleetSize; }
    public void setFleetSize(int fleetSize) { this.fleetSize = fleetSize; }

    public double getAlsFraction() { return alsFraction; }
    public void setAlsFraction(double alsFraction) { this.alsFraction = alsFraction; }

    public double getBaseDemandPerHour() { return baseDemandPerHour; }
    public void setBaseDemandPerHour(double baseDemandPerHour) { this.baseDemandPerHour = baseDemandPerHour; }

    public List<DemandProfile> getDemandProfiles() { return demandProfiles; }
    public void setDemandProfiles(List<DemandProfile> demandProfiles) { this.demandProfiles = demandProfiles; }

    public Map<String, Double> getSeverityDistribution() { return severityDistribution; }
    public void setSeverityDistribution(Map<String, Double> severityDistribution) { this.severityDistribution = severityDistribution; }

    public double getAlsRequiredFraction() { return alsRequiredFraction; }
    public void setAlsRequiredFraction(double alsRequiredFraction) { this.alsRequiredFraction = alsRequiredFraction; }

    public double getMinLat() { return minLat; }
    public void setMinLat(double minLat) { this.minLat = minLat; }
    public double getMaxLat() { return maxLat; }
    public void setMaxLat(double maxLat) { this.maxLat = maxLat; }
    public double getMinLon() { return minLon; }
    public void setMinLon(double minLon) { this.minLon = minLon; }
    public double getMaxLon() { return maxLon; }
    public void setMaxLon(double maxLon) { this.maxLon = maxLon; }

    public List<ZoneConfig> getZones() { return zones; }
    public void setZones(List<ZoneConfig> zones) { this.zones = zones; }

    public List<StationConfig> getStations() { return stations; }
    public void setStations(List<StationConfig> stations) { this.stations = stations; }

    public List<HospitalConfig> getHospitals() { return hospitals; }
    public void setHospitals(List<HospitalConfig> hospitals) { this.hospitals = hospitals; }

    public double getCoverageRadiusKm() { return coverageRadiusKm; }
    public void setCoverageRadiusKm(double coverageRadiusKm) { this.coverageRadiusKm = coverageRadiusKm; }

    public double getMeanSceneTimeSeconds() { return meanSceneTimeSeconds; }
    public void setMeanSceneTimeSeconds(double v) { this.meanSceneTimeSeconds = v; }
    public double getSceneTimeStdDev() { return sceneTimeStdDev; }
    public void setSceneTimeStdDev(double v) { this.sceneTimeStdDev = v; }
    public double getMeanHandoverSeconds() { return meanHandoverSeconds; }
    public void setMeanHandoverSeconds(double v) { this.meanHandoverSeconds = v; }
    public double getHandoverStdDev() { return handoverStdDev; }
    public void setHandoverStdDev(double v) { this.handoverStdDev = v; }
    public double getTravelNoiseStdDev() { return travelNoiseStdDev; }
    public void setTravelNoiseStdDev(double v) { this.travelNoiseStdDev = v; }
    public double getResponseTargetSeconds() { return responseTargetSeconds; }
    public void setResponseTargetSeconds(double v) { this.responseTargetSeconds = v; }

    public int getMaxRedeployMoves() { return maxRedeployMoves; }
    public void setMaxRedeployMoves(int v) { this.maxRedeployMoves = v; }
    public double getMinRedeployGain() { return minRedeployGain; }
    public void setMinRedeployGain(double v) { this.minRedeployGain = v; }
    public int getRedeployCooldownMinutes() { return redeployCooldownMinutes; }
    public void setRedeployCooldownMinutes(int v) { this.redeployCooldownMinutes = v; }

    /**
     * Time-of-day demand multiplier profile.
     */
    public static class DemandProfile {
        private int fromHour;
        private int toHour;
        private double factor;

        public DemandProfile() {}
        public DemandProfile(int fromHour, int toHour, double factor) {
            this.fromHour = fromHour;
            this.toHour = toHour;
            this.factor = factor;
        }

        public int getFromHour() { return fromHour; }
        public void setFromHour(int v) { this.fromHour = v; }
        public int getToHour() { return toHour; }
        public void setToHour(int v) { this.toHour = v; }
        public double getFactor() { return factor; }
        public void setFactor(double v) { this.factor = v; }
    }

    public static class ZoneConfig {
        private double lat;
        private double lon;
        private double demandPerHour = 1.0;

        public ZoneConfig() {}
        public ZoneConfig(double lat, double lon, double demandPerHour) {
            this.lat = lat; this.lon = lon; this.demandPerHour = demandPerHour;
        }

        public double getLat() { return lat; }
        public void setLat(double v) { this.lat = v; }
        public double getLon() { return lon; }
        public void setLon(double v) { this.lon = v; }
        public double getDemandPerHour() { return demandPerHour; }
        public void setDemandPerHour(double v) { this.demandPerHour = v; }

        public GeoPoint toGeoPoint() { return new GeoPoint(lat, lon); }
    }

    public static class StationConfig {
        private String name;
        private double lat;
        private double lon;
        private int units = 4;

        public StationConfig() {}
        public StationConfig(String name, double lat, double lon, int units) {
            this.name = name; this.lat = lat; this.lon = lon; this.units = units;
        }

        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public double getLat() { return lat; }
        public void setLat(double v) { this.lat = v; }
        public double getLon() { return lon; }
        public void setLon(double v) { this.lon = v; }
        public int getUnits() { return units; }
        public void setUnits(int v) { this.units = v; }

        public GeoPoint toGeoPoint() { return new GeoPoint(lat, lon); }
    }

    public static class HospitalConfig {
        private String name;
        private double lat;
        private double lon;
        private int edBeds = 20;
        private int icuBeds = 5;
        private int ventilators = 3;
        private List<String> capabilities = List.of("TRAUMA", "CARDIAC", "GENERAL");

        public HospitalConfig() {}

        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public double getLat() { return lat; }
        public void setLat(double v) { this.lat = v; }
        public double getLon() { return lon; }
        public void setLon(double v) { this.lon = v; }
        public int getEdBeds() { return edBeds; }
        public void setEdBeds(int v) { this.edBeds = v; }
        public int getIcuBeds() { return icuBeds; }
        public void setIcuBeds(int v) { this.icuBeds = v; }
        public int getVentilators() { return ventilators; }
        public void setVentilators(int v) { this.ventilators = v; }
        public List<String> getCapabilities() { return capabilities; }
        public void setCapabilities(List<String> v) { this.capabilities = v; }

        public GeoPoint toGeoPoint() { return new GeoPoint(lat, lon); }

        public Set<ClinicalNeed> toClinicalNeeds() {
            Set<ClinicalNeed> needs = EnumSet.noneOf(ClinicalNeed.class);
            for (String c : capabilities) {
                try { needs.add(ClinicalNeed.valueOf(c)); } catch (IllegalArgumentException ignored) {}
            }
            return needs;
        }
    }
}
