package com.h8.ems.hospital.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "capacity_snapshot", schema = "hospital")
public class CapacitySnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(name = "ed_beds_free", nullable = false)
    private int edBedsFree;

    @Column(name = "icu_beds_free", nullable = false)
    private int icuBedsFree;

    @Column(name = "ventilators_free", nullable = false)
    private int ventilatorsFree;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public CapacitySnapshotEntity() {
    }

    public CapacitySnapshotEntity(UUID id, UUID hospitalId, int edBedsFree, int icuBedsFree, int ventilatorsFree, Instant updatedAt) {
        this.id = id;
        this.hospitalId = hospitalId;
        this.edBedsFree = edBedsFree;
        this.icuBedsFree = icuBedsFree;
        this.ventilatorsFree = ventilatorsFree;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(UUID hospitalId) {
        this.hospitalId = hospitalId;
    }

    public int getEdBedsFree() {
        return edBedsFree;
    }

    public void setEdBedsFree(int edBedsFree) {
        this.edBedsFree = edBedsFree;
    }

    public int getIcuBedsFree() {
        return icuBedsFree;
    }

    public void setIcuBedsFree(int icuBedsFree) {
        this.icuBedsFree = icuBedsFree;
    }

    public int getVentilatorsFree() {
        return ventilatorsFree;
    }

    public void setVentilatorsFree(int ventilatorsFree) {
        this.ventilatorsFree = ventilatorsFree;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
