package com.h8.ems.dispatch.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.h8.ems.common.model.GeoPoint;
import com.h8.ems.common.model.UnitSnapshot;
import com.h8.ems.common.model.UnitStatus;
import com.h8.ems.common.model.UnitType;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an ambulance unit in dispatch.ambulance_unit.
 * Note: uses pure conditional SQL update for concurrency reservation without manual version collisions (Hard Rule #3 & Correction #4).
 */
@Entity
@Table(name = "ambulance_unit", schema = "dispatch")
public class AmbulanceUnitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "call_sign", nullable = false, unique = true, length = 16)
    private String callSign;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 8)
    private UnitType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private UnitStatus status = UnitStatus.AVAILABLE;

    @Column(name = "position", columnDefinition = "geography(Point, 4326)")
    private Point position;

    @Column(name = "position_at")
    private Instant positionAt;

    @Column(name = "shift_start")
    private Instant shiftStart;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_station_id")
    private StationEntity homeStation;

    @Column(name = "version", nullable = false)
    private long version = 0L;

    public AmbulanceUnitEntity() {
    }

    public AmbulanceUnitEntity(UUID id, String callSign, UnitType type, UnitStatus status,
                               Point position, Instant positionAt, Instant shiftStart,
                               StationEntity homeStation) {
        this.id = id;
        this.callSign = callSign;
        this.type = type;
        this.status = status;
        this.position = position;
        this.positionAt = positionAt;
        this.shiftStart = shiftStart;
        this.homeStation = homeStation;
        this.version = 0L;
    }

    /**
     * Converts to immutable domain snapshot for scoring with common library models.
     */
    public UnitSnapshot toSnapshot() {
        GeoPoint pos = null;
        if (position != null) {
            pos = new GeoPoint(position.getY(), position.getX());
        }
        GeoPoint stationPos = null;
        UUID stationId = null;
        if (homeStation != null) {
            stationId = homeStation.getId();
            if (homeStation.getLocation() != null) {
                stationPos = new GeoPoint(homeStation.getLocation().getY(), homeStation.getLocation().getX());
            }
        }
        return new UnitSnapshot(
                id,
                callSign,
                type,
                status,
                pos,
                positionAt,
                shiftStart,
                stationId,
                stationPos
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCallSign() { return callSign; }
    public void setCallSign(String callSign) { this.callSign = callSign; }

    public UnitType getType() { return type; }
    public void setType(UnitType type) { this.type = type; }

    public UnitStatus getStatus() { return status; }
    public void setStatus(UnitStatus status) { this.status = status; }

    @JsonIgnore
    public Point getPosition() { return position; }
    public void setPosition(Point position) { this.position = position; }

    @JsonProperty("lat")
    public Double getLat() {
        return position != null ? position.getY() : null;
    }

    @JsonProperty("lon")
    public Double getLon() {
        return position != null ? position.getX() : null;
    }

    public Instant getPositionAt() { return positionAt; }
    public void setPositionAt(Instant positionAt) { this.positionAt = positionAt; }

    public Instant getShiftStart() { return shiftStart; }
    public void setShiftStart(Instant shiftStart) { this.shiftStart = shiftStart; }

    public StationEntity getHomeStation() { return homeStation; }
    public void setHomeStation(StationEntity homeStation) { this.homeStation = homeStation; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
