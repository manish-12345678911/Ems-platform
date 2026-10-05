package com.h8.ems.routing.service;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.Profile;
import com.h8.ems.common.eta.EtaProvider;
import com.h8.ems.common.eta.HaversineEta;
import com.h8.ems.common.model.GeoPoint;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * GraphHopper-based ETA provider.
 * Loads OSM road network from configured graph location.
 * Applies time-of-day congestion factor.
 * Falls back cleanly to HaversineEta when graph is not loaded (Correction #10).
 */
@Component
public class GraphHopperEta implements EtaProvider {

    private static final Logger log = LoggerFactory.getLogger(GraphHopperEta.class);

    private final GraphHopper hopper;
    private final EtaProvider fallback;
    private final boolean loaded;

    public GraphHopperEta() {
        this("data/graph/map.osm.pbf", "data/graph/cache", new HaversineEta());
    }

    public GraphHopperEta(
            @Value("${h8.routing.osm-file:data/graph/map.osm.pbf}") String osmFile,
            @Value("${h8.routing.graph-location:data/graph/cache}") String graphLocation
    ) {
        this(osmFile, graphLocation, new HaversineEta());
    }

    public GraphHopperEta(String osmFile, String graphLocation, EtaProvider fallback) {
        this.fallback = fallback != null ? fallback : new HaversineEta();

        File osm = new File(osmFile);
        File graphDir = new File(graphLocation);

        if (osm.exists() || (graphDir.exists() && graphDir.isDirectory() && graphDir.list() != null && graphDir.list().length > 0)) {
            GraphHopper gh = null;
            boolean success = false;
            try {
                log.info("Initializing GraphHopper from OSM: {}, graphDir: {}", osmFile, graphLocation);
                gh = new GraphHopper();
                if (osm.exists()) {
                    gh.setOSMFile(osmFile);
                }
                gh.setGraphHopperLocation(graphLocation);
                gh.setProfiles(new Profile("car").setWeighting("fastest"));
                gh.importOrLoad();
                success = true;
                log.info("GraphHopper successfully initialized and loaded.");
            } catch (Exception e) {
                log.warn("Failed to initialize GraphHopper (falling back to HaversineEta): {}", e.getMessage());
                if (gh != null) {
                    try { gh.close(); } catch (Exception ignored) {}
                }
                gh = null;
                success = false;
            }
            this.hopper = gh;
            this.loaded = success;
        } else {
            log.info("No OSM file found at {} or graph cache at {}. GraphHopper disabled, using HaversineEta fallback.",
                    osmFile, graphLocation);
            this.hopper = null;
            this.loaded = false;
        }
    }

    @Override
    public double etaSeconds(GeoPoint from, GeoPoint to, Instant at) {
        if (!loaded || hopper == null) {
            return fallback.etaSeconds(from, to, at);
        }

        try {
            GHRequest req = new GHRequest(from.lat(), from.lon(), to.lat(), to.lon())
                    .setProfile("car");
            GHResponse rsp = hopper.route(req);

            if (rsp.hasErrors() || rsp.getAll().isEmpty()) {
                return fallback.etaSeconds(from, to, at);
            }

            ResponsePath path = rsp.getBest();
            double baseSeconds = path.getTime() / 1000.0;
            double factor = timeOfDayFactor(at);
            return baseSeconds * factor;
        } catch (Exception e) {
            log.warn("GraphHopper routing failed for {} -> {}: {}. Falling back to HaversineEta.", from, to, e.getMessage());
            return fallback.etaSeconds(from, to, at);
        }
    }

    /**
     * Time-of-day traffic factor based on hour of the day:
     * - Morning rush (07:30 - 09:30): 1.30
     * - Evening rush (16:30 - 18:30): 1.30
     * - Night free-flow (23:00 - 05:00): 0.90
     * - Other times: 1.00
     */
    public double timeOfDayFactor(Instant at) {
        if (at == null) return 1.0;
        ZonedDateTime zdt = at.atZone(ZoneOffset.UTC);
        int hour = zdt.getHour();
        int minute = zdt.getMinute();
        double timeOfDay = hour + (minute / 60.0);

        if ((timeOfDay >= 7.5 && timeOfDay <= 9.5) || (timeOfDay >= 16.5 && timeOfDay <= 18.5)) {
            return 1.30;
        } else if (timeOfDay >= 23.0 || timeOfDay < 5.0) {
            return 0.90;
        }
        return 1.00;
    }

    public boolean isLoaded() {
        return loaded;
    }

    @PreDestroy
    public void close() {
        if (hopper != null) {
            try {
                hopper.close();
            } catch (Exception e) {
                log.warn("Error closing GraphHopper: {}", e.getMessage());
            }
        }
    }
}
