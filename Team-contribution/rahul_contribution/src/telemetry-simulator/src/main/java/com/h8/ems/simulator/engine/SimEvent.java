package com.h8.ems.simulator.engine;

import java.time.Instant;

/**
 * Sealed interface for discrete simulation events.
 * Each event has a time and is processed by the SimEngine in chronological order.
 */
public sealed interface SimEvent extends Comparable<SimEvent> {

    Instant time();

    @Override
    default int compareTo(SimEvent other) {
        return this.time().compareTo(other.time());
    }

    /**
     * A new incident has arrived and needs dispatching.
     */
    record IncidentArrived(
            Instant time,
            java.util.UUID incidentId
    ) implements SimEvent {}

    /**
     * A unit has been dispatched and is en route to the scene.
     */
    record UnitDispatched(
            Instant time,
            java.util.UUID incidentId,
            java.util.UUID unitId,
            double travelSeconds
    ) implements SimEvent {}

    /**
     * A unit has arrived at the incident scene.
     */
    record UnitArrivedScene(
            Instant time,
            java.util.UUID incidentId,
            java.util.UUID unitId
    ) implements SimEvent {}

    /**
     * A unit is transporting the patient to hospital.
     */
    record UnitTransporting(
            Instant time,
            java.util.UUID incidentId,
            java.util.UUID unitId,
            java.util.UUID hospitalId,
            double transportSeconds
    ) implements SimEvent {}

    /**
     * A unit has arrived at the hospital.
     */
    record UnitArrivedHospital(
            Instant time,
            java.util.UUID incidentId,
            java.util.UUID unitId,
            java.util.UUID hospitalId
    ) implements SimEvent {}

    /**
     * A unit is back available (after handover).
     */
    record UnitAvailable(
            Instant time,
            java.util.UUID unitId
    ) implements SimEvent {}

    /**
     * Periodic redeployment check.
     */
    record RedeployCheck(
            Instant time
    ) implements SimEvent {}
}
