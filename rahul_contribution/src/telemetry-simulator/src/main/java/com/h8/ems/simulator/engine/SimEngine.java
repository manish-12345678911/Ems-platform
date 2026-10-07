package com.h8.ems.simulator.engine;

import com.h8.ems.common.model.*;
import com.h8.ems.common.scoring.*;
import com.h8.ems.simulator.config.ScenarioConfig;
import com.h8.ems.simulator.policy.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Discrete Event Simulation engine.
 * Processes SimEvents from a PriorityQueue in chronological order.
 */
public final class SimEngine {

    private static final Logger log = LoggerFactory.getLogger(SimEngine.class);

    private final PriorityQueue<SimEvent> eventQueue = new PriorityQueue<>();
    private final SimState state;
    private final TravelModel travelModel;
    private final HospitalModel hospitalModel;
    private final Policy policy;
    private final ScenarioConfig config;
    private final RedeploymentPlanner redeployPlanner;
    private final CoverageModel coverageModel;
    private final List<IncidentResult> results = new ArrayList<>();

    public SimEngine(SimState state, TravelModel travelModel, HospitalModel hospitalModel,
                     Policy policy, ScenarioConfig config,
                     CoverageModel coverageModel, RedeploymentPlanner redeployPlanner) {
        this.state = state;
        this.travelModel = travelModel;
        this.hospitalModel = hospitalModel;
        this.policy = policy;
        this.config = config;
        this.coverageModel = coverageModel;
        this.redeployPlanner = redeployPlanner;
    }

    /**
     * Schedule all incident arrivals from pre-sampled draws.
     */
    public void scheduleIncidents(List<IncidentDraw> draws) {
        for (IncidentDraw draw : draws) {
            state.registerDraw(draw);
            eventQueue.add(new SimEvent.IncidentArrived(draw.arrivalTime(), draw.incidentId()));
        }
    }

    /**
     * Run the simulation until the event queue is empty.
     */
    public List<IncidentResult> run() {
        while (!eventQueue.isEmpty()) {
            SimEvent event = eventQueue.poll();
            state.advanceClock(event.time());
            processEvent(event);
        }
        return Collections.unmodifiableList(results);
    }

    private void processEvent(SimEvent event) {
        switch (event) {
            case SimEvent.IncidentArrived e -> handleIncidentArrived(e);
            case SimEvent.UnitDispatched e -> handleUnitDispatched(e);
            case SimEvent.UnitArrivedScene e -> handleUnitArrivedScene(e);
            case SimEvent.UnitTransporting e -> handleUnitTransporting(e);
            case SimEvent.UnitArrivedHospital e -> handleUnitArrivedHospital(e);
            case SimEvent.UnitAvailable e -> handleUnitAvailable(e);
            case SimEvent.RedeployCheck e -> handleRedeployCheck(e);
        }
    }

    private void handleIncidentArrived(SimEvent.IncidentArrived e) {
        IncidentDraw draw = state.getDraw(e.incidentId());
        if (draw == null) return;

        state.addActiveIncident(e.incidentId(), draw);

        UUID unitId = policy.selectUnit(draw, state, travelModel);
        if (unitId == null) {
            // No unit available — incident goes unserved
            log.debug("No unit available for incident {}", e.incidentId());
            SimState.ActiveIncident ai = state.getActiveIncident(e.incidentId());
            results.add(IncidentResult.unserved(draw, policy.name()));
            state.removeActiveIncident(e.incidentId());
            return;
        }

        UnitSnapshot unit = state.unitSnapshot(unitId);
        state.dispatchUnit(unitId, draw.location());

        SimState.ActiveIncident ai = state.getActiveIncident(e.incidentId());
        ai.assignedUnitId = unitId;
        ai.dispatchedAt = state.clock();
        ai.unitCallSign = unit.callSign();
        ai.unitType = unit.type();

        double travelSec = travelModel.travelSeconds(
                unit.position(), draw.location(), state.clock(), draw.travelNoiseFactor());

        eventQueue.add(new SimEvent.UnitArrivedScene(
                state.clock().plus((long)(travelSec * 1000), ChronoUnit.MILLIS),
                e.incidentId(), unitId));
    }

    private void handleUnitDispatched(SimEvent.UnitDispatched e) {
        // Handled inline in IncidentArrived
    }

    private void handleUnitArrivedScene(SimEvent.UnitArrivedScene e) {
        state.unitArrivedScene(e.unitId());
        state.moveUnit(e.unitId(), state.getDraw(e.incidentId()).location());

        SimState.ActiveIncident ai = state.getActiveIncident(e.incidentId());
        if (ai != null) {
            ai.arrivedSceneAt = state.clock();
        }

        IncidentDraw draw = state.getDraw(e.incidentId());
        double sceneTime = draw.sceneTimeSeconds();

        // After scene time, decide: transport or treat on scene
        boolean needsTransport = draw.severity() == Severity.CRITICAL
                || draw.severity() == Severity.EMERGENCY
                || draw.requiresAls();

        if (needsTransport) {
            UUID hospitalId = policy.selectHospital(draw, state, travelModel, hospitalModel);
            if (ai != null) {
                ai.destinationHospitalId = hospitalId;
                HospitalSnapshot hs = hospitalModel.snapshot(hospitalId);
                ai.destinationHospitalName = hs != null ? hs.name() : "unknown";
            }

            state.unitTransporting(e.unitId());
            Instant transportStart = state.clock().plus((long)(sceneTime * 1000), ChronoUnit.MILLIS);
            if (ai != null) ai.transportStartedAt = transportStart;

            HospitalSnapshot hospital = hospitalModel.snapshot(hospitalId);
            GeoPoint hospLoc = hospital != null ? hospital.location() : draw.location();
            double transportSec = travelModel.travelSeconds(
                    draw.location(), hospLoc, transportStart, draw.travelNoiseFactor());

            eventQueue.add(new SimEvent.UnitArrivedHospital(
                    transportStart.plus((long)(transportSec * 1000), ChronoUnit.MILLIS),
                    e.incidentId(), e.unitId(), hospitalId));
        } else {
            // Treated on scene — unit returns available
            Instant availableAt = state.clock().plus((long)(sceneTime * 1000), ChronoUnit.MILLIS);
            eventQueue.add(new SimEvent.UnitAvailable(availableAt, e.unitId()));
            completeIncident(e.incidentId());
        }
    }

    private void handleUnitTransporting(SimEvent.UnitTransporting e) {
        // Handled inline
    }

    private void handleUnitArrivedHospital(SimEvent.UnitArrivedHospital e) {
        HospitalSnapshot hospital = hospitalModel.snapshot(e.hospitalId());
        GeoPoint hospLoc = hospital != null ? hospital.location() : new GeoPoint(0, 0);
        state.unitAtHospital(e.unitId(), hospLoc);
        hospitalModel.admitPatient(e.hospitalId());

        SimState.ActiveIncident ai = state.getActiveIncident(e.incidentId());
        if (ai != null) ai.arrivedHospitalAt = state.clock();

        IncidentDraw draw = state.getDraw(e.incidentId());
        double handoverSec = draw.handoverSeconds();

        Instant handoverDone = state.clock().plus((long)(handoverSec * 1000), ChronoUnit.MILLIS);
        if (ai != null) ai.handedOverAt = handoverDone;

        eventQueue.add(new SimEvent.UnitAvailable(handoverDone, e.unitId()));

        // Release bed after handover
        hospitalModel.releasePatient(e.hospitalId());

        completeIncident(e.incidentId());
    }

    private void handleUnitAvailable(SimEvent.UnitAvailable e) {
        state.unitAvailable(e.unitId());

        if (policy.usesRedeployment()) {
            eventQueue.add(new SimEvent.RedeployCheck(state.clock()));
        }
    }

    private void handleRedeployCheck(SimEvent.RedeployCheck e) {
        if (redeployPlanner == null || coverageModel == null) return;

        List<UnitSnapshot> idle = state.availableUnits();
        List<GeoPoint> standbyPoints = state.allUnitSnapshots().stream()
                .map(UnitSnapshot::homeStationLocation)
                .distinct().toList();

        var moves = redeployPlanner.plan(idle, standbyPoints,
                RedeploymentPlanner.PlannerLimits.defaults());

        for (var move : moves) {
            state.moveUnit(move.unitId(), move.target());
        }
    }

    private void completeIncident(UUID incidentId) {
        SimState.ActiveIncident ai = state.getActiveIncident(incidentId);
        if (ai == null) return;

        IncidentDraw draw = ai.draw;
        double responseTimeSec = 0;
        if (ai.arrivedSceneAt != null && draw.arrivalTime() != null) {
            responseTimeSec = (ai.arrivedSceneAt.toEpochMilli() - draw.arrivalTime().toEpochMilli()) / 1000.0;
        }

        results.add(new IncidentResult(
                draw.incidentId(),
                draw.severity().name(),
                draw.need().name(),
                draw.arrivalTime(),
                ai.dispatchedAt,
                ai.arrivedSceneAt,
                ai.transportStartedAt,
                ai.arrivedHospitalAt,
                ai.handedOverAt,
                responseTimeSec,
                ai.unitCallSign,
                ai.unitType != null ? ai.unitType.name() : "",
                ai.destinationHospitalName != null ? ai.destinationHospitalName : "",
                policy.name(),
                true
        ));

        state.removeActiveIncident(incidentId);
    }

    /**
     * Result record for one incident.
     */
    public record IncidentResult(
            UUID incidentId, String severity, String need,
            Instant receivedAt, Instant dispatchedAt, Instant arrivedSceneAt,
            Instant transportStartedAt, Instant arrivedHospitalAt, Instant handedOverAt,
            double responseTimeSec, String unitCallSign, String unitType,
            String hospitalName, String policyName, boolean served
    ) {
        static IncidentResult unserved(IncidentDraw draw, String policyName) {
            return new IncidentResult(draw.incidentId(), draw.severity().name(), draw.need().name(),
                    draw.arrivalTime(), null, null, null, null, null,
                    -1, "", "", "", policyName, false);
        }

        public String toCsvRow(String scenario, long seed) {
            return "%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%.1f,%s,%s,%s".formatted(
                    scenario, policyName, seed, incidentId,
                    severity, need,
                    fmt(receivedAt), fmt(dispatchedAt), fmt(arrivedSceneAt),
                    fmt(transportStartedAt), fmt(arrivedHospitalAt), fmt(handedOverAt),
                    responseTimeSec, unitCallSign, unitType, hospitalName);
        }

        private static String fmt(Instant t) { return t == null ? "" : t.toString(); }

        public static String csvHeader() {
            return "scenario,policy,seed,incidentId,severity,need,receivedAt,dispatchedAt," +
                    "arrivedSceneAt,transportStartedAt,arrivedHospitalAt,handedOverAt," +
                    "responseTimeSec,unitCallSign,unitType,hospitalName";
        }
    }
}
