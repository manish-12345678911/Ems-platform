package com.h8.ems.hospital.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.h8.ems.common.model.ClinicalNeed;
import com.h8.ems.common.model.GeoPoint;
import com.h8.ems.common.model.HospitalSnapshot;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "hospital", schema = "hospital")
public class HospitalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 256)
    private String name;

    @JsonIgnore
    @Column(name = "location", columnDefinition = "geography(Point,4326)", nullable = false)
    private Point location;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hospital_capability", schema = "hospital",
            joinColumns = @JoinColumn(name = "hospital_id"))
    @Column(name = "need", length = 16)
    @Enumerated(EnumType.STRING)
    private Set<ClinicalNeed> capabilities = new HashSet<>();

    public HospitalEntity() {
    }

    public HospitalEntity(UUID id, String name, Point location, Set<ClinicalNeed> capabilities) {
        this.id = id;
        this.name = name;
        this.location = location;
        if (capabilities != null) {
            this.capabilities.addAll(capabilities);
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    @JsonProperty("lat")
    public Double getLat() {
        return location != null ? location.getY() : null;
    }

    @JsonProperty("lon")
    public Double getLon() {
        return location != null ? location.getX() : null;
    }

    public Set<ClinicalNeed> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Set<ClinicalNeed> capabilities) {
        this.capabilities = capabilities != null ? capabilities : new HashSet<>();
    }

    public HospitalSnapshot toSnapshot(int edBedsFree, int icuBedsFree, int ventilatorsFree, Instant capacityUpdatedAt) {
        GeoPoint geoPoint = location != null ? new GeoPoint(location.getY(), location.getX()) : null;
        return new HospitalSnapshot(
                id,
                name,
                geoPoint,
                new HashSet<>(capabilities),
                edBedsFree,
                icuBedsFree,
                ventilatorsFree,
                capacityUpdatedAt
        );
    }
}
