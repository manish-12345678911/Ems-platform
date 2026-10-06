package com.h8.ems.common.scoring;

import com.h8.ems.common.model.GeoPoint;
import com.h8.ems.common.model.HospitalSnapshot;
import com.h8.ems.common.model.IncidentSnapshot;

import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Ranks destination hospitals for a given incident.
 * Factors: transport ETA, estimated wait time, stale capacity penalty.
 * Per architecture section 4 and correction #8.
 */
public final class DestinationRanker {

    private static final double ETA_WEIGHT = 0.5;
    private static final double WAIT_WEIGHT = 0.3;
    private static final double STALE_PENALTY_MINUTES = 7.0;

    /**
     * Ranks hospitals for a given incident, from best (lowest score) to worst.
     *
     * @param incident     the incident
     * @param hospitals    candidate hospitals
     * @param transportEta function to compute ETA from incident to each hospital
     * @param now          current time
     * @param ttlMinutes   capacity freshness TTL
     * @return ranked list of hospital results
     */
    public List<RankedHospital> rank(IncidentSnapshot incident,
                                      List<HospitalSnapshot> hospitals,
                                      BiFunction<GeoPoint, GeoPoint, Double> transportEta,
                                      Instant now, int ttlMinutes) {
        List<RankedHospital> results = new ArrayList<>();

        for (HospitalSnapshot h : hospitals) {
            // Filter by capability
            if (!h.supports(incident.need())) continue;

            double etaSeconds = transportEta.apply(incident.location(), h.location());
            double etaMinutes = etaSeconds / 60.0;
            double waitMinutes = h.estimatedWaitMinutes();
            boolean stale = h.isCapacityStale(now, ttlMinutes);

            // Per correction #8: stale capacity gets an unknown penalty,
            // but we ensure it doesn't make unknown hospitals beat known-full ones
            double stalePenalty = stale ? STALE_PENALTY_MINUTES : 0.0;

            double score = ETA_WEIGHT * etaMinutes
                    + WAIT_WEIGHT * (waitMinutes + stalePenalty);

            results.add(new RankedHospital(h, score, etaSeconds, waitMinutes, stale));
        }

        results.sort(Comparator.comparingDouble(RankedHospital::score));
        return Collections.unmodifiableList(results);
    }

    /**
     * A ranked hospital result with score breakdown.
     */
    public record RankedHospital(
            HospitalSnapshot hospital,
            double score,
            double transportEtaSeconds,
            double estimatedWaitMinutes,
            boolean capacityStale
    ) {
    }
}
