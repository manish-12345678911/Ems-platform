package com.h8.ems.incident.model;

import com.h8.ems.common.model.ClinicalNeed;
import com.h8.ems.common.model.IncidentStatus;
import com.h8.ems.common.model.Severity;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an incident in incident.incident.
 */
@Entity
@Table(name = "incident", schema = "incident")
public class IncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "location", nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point location;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "need", nullable = false, length = 16)
    private ClinicalNeed need;

    @Column(name = "requires_als", nullable = false)
    private boolean requiresAls;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private IncidentStatus status;

    @Column(name = "assigned_unit_id")
    private UUID assignedUnitId;

    @Column(name = "destination_hospital_id")
    private UUID destinationHospitalId;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "arrived_scene_at")
    private Instant arrivedSceneAt;

    @Column(name = "arrived_hospital_at")
    private Instant arrivedHospitalAt;

    @Column(name = "handed_over_at")
    private Instant handedOverAt;

    @Column(name = "caller_hash", columnDefinition = "CHAR(64)")
    private String callerHash;

    public IncidentEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public Point getLocation() { return location; }
    public void setLocation(Point location) { this.location = location; }

    @com.fasterxml.jackson.annotation.JsonProperty("lat")
    public double getLat() {
        return location != null ? location.getY() : 0.0;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("lon")
    public double getLon() {
        return location != null ? location.getX() : 0.0;
    }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public ClinicalNeed getNeed() { return need; }
    public void setNeed(ClinicalNeed need) { this.need = need; }

    public boolean isRequiresAls() { return requiresAls; }
    public void setRequiresAls(boolean requiresAls) { this.requiresAls = requiresAls; }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public UUID getAssignedUnitId() { return assignedUnitId; }
    public void setAssignedUnitId(UUID assignedUnitId) { this.assignedUnitId = assignedUnitId; }

    public UUID getDestinationHospitalId() { return destinationHospitalId; }
    public void setDestinationHospitalId(UUID destinationHospitalId) { this.destinationHospitalId = destinationHospitalId; }

    public Instant getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(Instant dispatchedAt) { this.dispatchedAt = dispatchedAt; }

    public Instant getArrivedSceneAt() { return arrivedSceneAt; }
    public void setArrivedSceneAt(Instant arrivedSceneAt) { this.arrivedSceneAt = arrivedSceneAt; }

    public Instant getArrivedHospitalAt() { return arrivedHospitalAt; }
    public void setArrivedHospitalAt(Instant arrivedHospitalAt) { this.arrivedHospitalAt = arrivedHospitalAt; }

    public Instant getHandedOverAt() { return handedOverAt; }
    public void setHandedOverAt(Instant handedOverAt) { this.handedOverAt = handedOverAt; }

    public String getCallerHash() { return callerHash; }
    public void setCallerHash(String callerHash) { this.callerHash = callerHash; }
}
