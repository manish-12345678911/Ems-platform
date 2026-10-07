package com.h8.ems.simulator.engine;

import com.h8.ems.common.model.*;
import com.h8.ems.simulator.config.ScenarioConfig;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Generates incidents via non-homogeneous Poisson process (NHPP) using thinning.
 * Zone selection is weighted by demand_per_hour (zone CDF).
 * All random draws are pre-sampled per incident for common random numbers.
 */
public final class DemandGenerator {

    private final ScenarioConfig config;
    private final Random rng;

    public DemandGenerator(ScenarioConfig config, long seed) {
        this.config = config;
        this.rng = new Random(seed);
    }

    /**
     * Generates all incidents for the scenario duration.
     * Returns a deterministic, pre-sampled list of IncidentDraw objects.
     */
    public List<IncidentDraw> generate(Instant simStart) {
        List<IncidentDraw> draws = new ArrayList<>();
        double durationSeconds = config.getDurationHours() * 3600.0;
        double maxRate = computeMaxRate();

        double t = 0;
        while (t < durationSeconds) {
            // Thinning: generate from homogeneous Poisson with max rate
            double interArrival = -Math.log(1 - rng.nextDouble()) / (maxRate / 3600.0);
            t += interArrival;
            if (t >= durationSeconds) break;

            // Accept/reject based on actual rate at time t
            double currentRate = rateAtTime(t) / 3600.0;
            if (rng.nextDouble() < currentRate / (maxRate / 3600.0)) {
                Instant arrivalTime = simStart.plus((long) (t * 1000), ChronoUnit.MILLIS);
                draws.add(sampleIncident(arrivalTime));
            }
        }

        return Collections.unmodifiableList(draws);
    }

    private IncidentDraw sampleIncident(Instant arrivalTime) {
        UUID id = new UUID(rng.nextLong(), rng.nextLong());
        GeoPoint location = sampleLocation();
        Severity severity = sampleSeverity();
        ClinicalNeed need = sampleNeed();
        boolean requiresAls = rng.nextDouble() < config.getAlsRequiredFraction();

        // Pre-sample random draws for common random numbers
        double sceneTime = Math.max(120, config.getMeanSceneTimeSeconds()
                + rng.nextGaussian() * config.getSceneTimeStdDev());
        double travelNoise = Math.exp(rng.nextGaussian() * config.getTravelNoiseStdDev());
        double handover = Math.max(300, config.getMeanHandoverSeconds()
                + rng.nextGaussian() * config.getHandoverStdDev());

        return new IncidentDraw(id, arrivalTime, location, severity, need,
                requiresAls, sceneTime, travelNoise, handover);
    }

    private GeoPoint sampleLocation() {
        List<ScenarioConfig.ZoneConfig> zones = config.getZones();
        if (zones == null || zones.isEmpty()) {
            // Uniform random within bounding box
            double lat = config.getMinLat() + rng.nextDouble() * (config.getMaxLat() - config.getMinLat());
            double lon = config.getMinLon() + rng.nextDouble() * (config.getMaxLon() - config.getMinLon());
            return new GeoPoint(lat, lon);
        }

        // Weighted zone CDF
        double totalDemand = zones.stream().mapToDouble(ScenarioConfig.ZoneConfig::getDemandPerHour).sum();
        double pick = rng.nextDouble() * totalDemand;
        double cumulative = 0;
        ScenarioConfig.ZoneConfig chosen = zones.getLast();
        for (var zone : zones) {
            cumulative += zone.getDemandPerHour();
            if (pick <= cumulative) {
                chosen = zone;
                break;
            }
        }
        // Jitter around zone centroid (~500m)
        double jitterLat = (rng.nextGaussian() * 0.005);
        double jitterLon = (rng.nextGaussian() * 0.005);
        return new GeoPoint(chosen.getLat() + jitterLat, chosen.getLon() + jitterLon);
    }

    private Severity sampleSeverity() {
        Map<String, Double> dist = config.getSeverityDistribution();
        double pick = rng.nextDouble();
        double cumulative = 0;
        for (var entry : dist.entrySet()) {
            cumulative += entry.getValue();
            if (pick <= cumulative) {
                return Severity.valueOf(entry.getKey());
            }
        }
        return Severity.LOW;
    }

    private ClinicalNeed sampleNeed() {
        ClinicalNeed[] values = ClinicalNeed.values();
        return values[rng.nextInt(values.length)];
    }

    private double computeMaxRate() {
        double maxFactor = config.getDemandProfiles().stream()
                .mapToDouble(ScenarioConfig.DemandProfile::getFactor)
                .max().orElse(1.0);
        return config.getBaseDemandPerHour() * maxFactor;
    }

    /**
     * Rate at simulation time t (in seconds from start).
     */
    private double rateAtTime(double tSeconds) {
        int hour = (int) ((tSeconds / 3600.0) % 24);
        double factor = config.getDemandProfiles().stream()
                .filter(p -> hour >= p.getFromHour() && hour < p.getToHour())
                .mapToDouble(ScenarioConfig.DemandProfile::getFactor)
                .findFirst().orElse(1.0);
        return config.getBaseDemandPerHour() * factor;
    }
}
