package com.h8.ems.simulator.engine;

import com.h8.ems.common.model.*;

import java.time.Instant;
import java.util.*;

/**
 * Hospital model tracking occupancy, handover times, and alert effects.
 */
public final class HospitalModel {

    private final Map<UUID, HospitalState> states = new HashMap<>();

    public void initHospital(UUID id, String name, GeoPoint location,
                              Set<ClinicalNeed> capabilities,
                              int edBeds, int icuBeds, int ventilators) {
        states.put(id, new HospitalState(id, name, location, capabilities,
                edBeds, icuBeds, ventilators, edBeds, icuBeds, ventilators, Instant.EPOCH));
    }

    /**
     * Occupy a bed (patient arrives). Returns handover delay if full.
     */
    public void admitPatient(UUID hospitalId) {
        HospitalState s = states.get(hospitalId);
        if (s != null && s.edBedsFree > 0) {
            s.edBedsFree--;
        }
    }

    /**
     * Release a bed (patient discharged or transferred).
     */
    public void releasePatient(UUID hospitalId) {
        HospitalState s = states.get(hospitalId);
        if (s != null && s.edBedsFree < s.totalEdBeds) {
            s.edBedsFree++;
        }
    }

    /**
     * Update the capacity timestamp (simulates ED nurse updating capacity).
     */
    public void updateCapacityTimestamp(UUID hospitalId, Instant now) {
        HospitalState s = states.get(hospitalId);
        if (s != null) {
            s.lastCapacityUpdate = now;
        }
    }

    /**
     * Get a snapshot for scoring/ranking.
     */
    public HospitalSnapshot snapshot(UUID hospitalId) {
        HospitalState s = states.get(hospitalId);
        if (s == null) return null;
        return new HospitalSnapshot(s.id, s.name, s.location, s.capabilities,
                s.edBedsFree, s.icuBedsFree, s.ventilatorsFree, s.lastCapacityUpdate);
    }

    /**
     * Get all hospital snapshots.
     */
    public List<HospitalSnapshot> allSnapshots() {
        return states.values().stream()
                .map(s -> new HospitalSnapshot(s.id, s.name, s.location, s.capabilities,
                        s.edBedsFree, s.icuBedsFree, s.ventilatorsFree, s.lastCapacityUpdate))
                .toList();
    }

    public List<UUID> allIds() {
        return new ArrayList<>(states.keySet());
    }

    /**
     * Mutable internal state for a hospital.
     */
    private static class HospitalState {
        final UUID id;
        final String name;
        final GeoPoint location;
        final Set<ClinicalNeed> capabilities;
        final int totalEdBeds;
        int edBedsFree;
        int icuBedsFree;
        int ventilatorsFree;
        Instant lastCapacityUpdate;

        HospitalState(UUID id, String name, GeoPoint location, Set<ClinicalNeed> capabilities,
                      int totalEdBeds, int icuBedsFree, int ventilatorsFree,
                      int edBedsFree, int icuBedsFreeInit, int ventilatorsFreeInit,
                      Instant lastCapacityUpdate) {
            this.id = id;
            this.name = name;
            this.location = location;
            this.capabilities = capabilities;
            this.totalEdBeds = totalEdBeds;
            this.edBedsFree = edBedsFree;
            this.icuBedsFree = icuBedsFreeInit;
            this.ventilatorsFree = ventilatorsFreeInit;
            this.lastCapacityUpdate = lastCapacityUpdate;
        }
    }
}
