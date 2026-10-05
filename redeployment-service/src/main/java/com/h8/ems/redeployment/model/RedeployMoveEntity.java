package com.h8.ems.redeployment.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "redeploy_move", schema = "redeploy")
public class RedeployMoveEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "target", nullable = false, length = 256)
    private String target;

    @Column(name = "coverage_gain", nullable = false)
    private double coverageGain;

    @Column(name = "accepted", nullable = false)
    private boolean accepted = false;

    @Column(name = "at", nullable = false)
    private Instant at = Instant.now();

    public RedeployMoveEntity() {
    }

    public RedeployMoveEntity(UUID id, UUID unitId, String target, double coverageGain, boolean accepted, Instant at) {
        this.id = id;
        this.unitId = unitId;
        this.target = target;
        this.coverageGain = coverageGain;
        this.accepted = accepted;
        this.at = at != null ? at : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUnitId() {
        return unitId;
    }

    public void setUnitId(UUID unitId) {
        this.unitId = unitId;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public double getCoverageGain() {
        return coverageGain;
    }

    public void setCoverageGain(double coverageGain) {
        this.coverageGain = coverageGain;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public Instant getAt() {
        return at;
    }

    public void setAt(Instant at) {
        this.at = at;
    }
}
