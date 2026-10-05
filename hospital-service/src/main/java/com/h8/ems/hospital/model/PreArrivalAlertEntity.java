package com.h8.ems.hospital.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pre_arrival_alert", schema = "hospital")
public class PreArrivalAlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(name = "eta_seconds", nullable = false)
    private int etaSeconds;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    @Column(name = "acked_at")
    private Instant ackedAt;

    public PreArrivalAlertEntity() {
    }

    public PreArrivalAlertEntity(UUID id, UUID incidentId, UUID hospitalId, int etaSeconds, Instant sentAt, Instant ackedAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.hospitalId = hospitalId;
        this.etaSeconds = etaSeconds;
        this.sentAt = sentAt != null ? sentAt : Instant.now();
        this.ackedAt = ackedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(UUID incidentId) {
        this.incidentId = incidentId;
    }

    public UUID getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(UUID hospitalId) {
        this.hospitalId = hospitalId;
    }

    public int getEtaSeconds() {
        return etaSeconds;
    }

    public void setEtaSeconds(int etaSeconds) {
        this.etaSeconds = etaSeconds;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getAckedAt() {
        return ackedAt;
    }

    public void setAckedAt(Instant ackedAt) {
        this.ackedAt = ackedAt;
    }
}
