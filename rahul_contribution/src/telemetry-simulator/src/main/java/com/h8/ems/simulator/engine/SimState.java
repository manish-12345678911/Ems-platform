package com.h8.ems.simulator.engine;

import com.h8.ems.common.model.*;

import java.time.Instant;
import java.util.*;

/**
 * Mutable simulation state: units, active incidents, clock.
 */
public final class SimState {

    private final Map<UUID, MutableUnit> units = new LinkedHashMap<>();
    private final Map<UUID, ActiveIncident> activeIncidents = new LinkedHashMap<>();
    private final Map<UUID, IncidentDraw> drawIndex = new HashMap<>();
    private Instant clock;

    public SimState(Instant startTime) {
        this.clock = startTime;
    }

    // --- Unit management ---

    public void addUnit(UUID id, String callSign, UnitType type, GeoPoint homeStation, UUID stationId) {
        units.put(id, new MutableUnit(id, callSign, type, UnitStatus.AVAILABLE,
                homeStation, homeStation, clock, clock, stationId));
    }

    public List<UnitSnapshot> availableUnits() {
        return units.values().stream()
                .filter(u -> u.status == UnitStatus.AVAILABLE)
                .map(this::toSnapshot)
                .toList();
    }

    public List<UnitSnapshot> allUnitSnapshots() {
        return units.values().stream().map(this::toSnapshot).toList();
    }

    public UnitSnapshot unitSnapshot(UUID unitId) {
        MutableUnit u = units.get(unitId);
        return u == null ? null : toSnapshot(u);
    }

    public void dispatchUnit(UUID unitId, GeoPoint destination) {
        MutableUnit u = units.get(unitId);
        if (u != null) {
            u.status = UnitStatus.DISPATCHED;
        }
    }

    public void unitArrivedScene(UUID unitId) {
        MutableUnit u = units.get(unitId);
        if (u != null) u.status = UnitStatus.ON_SCENE;
    }

    public void unitTransporting(UUID unitId) {
        MutableUnit u = units.get(unitId);
        if (u != null) u.status = UnitStatus.TRANSPORTING;
    }

    public void unitAtHospital(UUID unitId, GeoPoint hospitalLocation) {
        MutableUnit u = units.get(unitId);
        if (u != null) {
            u.status = UnitStatus.AT_HOSPITAL;
            u.position = hospitalLocation;
            u.positionAt = clock;
        }
    }

    public void unitAvailable(UUID unitId) {
        MutableUnit u = units.get(unitId);
        if (u != null) {
            u.status = UnitStatus.AVAILABLE;
            u.positionAt = clock;
        }
    }

    public void moveUnit(UUID unitId, GeoPoint newPosition) {
        MutableUnit u = units.get(unitId);
        if (u != null) {
            u.position = newPosition;
            u.positionAt = clock;
        }
    }

    // --- Incident management ---

    public void registerDraw(IncidentDraw draw) {
        drawIndex.put(draw.incidentId(), draw);
    }

    public IncidentDraw getDraw(UUID incidentId) {
        return drawIndex.get(incidentId);
    }

    public void addActiveIncident(UUID incidentId, IncidentDraw draw) {
        activeIncidents.put(incidentId, new ActiveIncident(incidentId, draw));
    }

    public ActiveIncident getActiveIncident(UUID incidentId) {
        return activeIncidents.get(incidentId);
    }

    public void removeActiveIncident(UUID incidentId) {
        activeIncidents.remove(incidentId);
    }

    // --- Clock ---

    public Instant clock() { return clock; }

    public void advanceClock(Instant newTime) {
        this.clock = newTime;
    }

    // --- Internal ---

    private UnitSnapshot toSnapshot(MutableUnit u) {
        return new UnitSnapshot(u.id, u.callSign, u.type, u.status,
                u.position, u.positionAt, u.shiftStart, u.stationId, u.homeStation);
    }

    /**
     * Mutable unit state — internal only.
     */
    static class MutableUnit {
        final UUID id;
        final String callSign;
        final UnitType type;
        UnitStatus status;
        GeoPoint position;
        final GeoPoint homeStation;
        Instant positionAt;
        final Instant shiftStart;
        final UUID stationId;

        MutableUnit(UUID id, String callSign, UnitType type, UnitStatus status,
                    GeoPoint position, GeoPoint homeStation, Instant positionAt,
                    Instant shiftStart, UUID stationId) {
            this.id = id;
            this.callSign = callSign;
            this.type = type;
            this.status = status;
            this.position = position;
            this.homeStation = homeStation;
            this.positionAt = positionAt;
            this.shiftStart = shiftStart;
            this.stationId = stationId;
        }
    }

    /**
     * Tracks an active incident through its lifecycle.
     */
    public static class ActiveIncident {
        public final UUID incidentId;
        public final IncidentDraw draw;
        public UUID assignedUnitId;
        public UUID destinationHospitalId;
        public String destinationHospitalName;
        public Instant dispatchedAt;
        public Instant arrivedSceneAt;
        public Instant transportStartedAt;
        public Instant arrivedHospitalAt;
        public Instant handedOverAt;
        public String unitCallSign;
        public UnitType unitType;

        ActiveIncident(UUID incidentId, IncidentDraw draw) {
            this.incidentId = incidentId;
            this.draw = draw;
        }
    }
}
