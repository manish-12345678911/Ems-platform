package com.h8.ems.dispatch.model;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.util.UUID;

@Entity
@Table(name = "station", schema = "dispatch")
public class StationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "location", nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point location;

    public StationEntity() {}

    public StationEntity(UUID id, String name, Point location) {
        this.id = id;
        this.name = name;
        this.location = location;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Point getLocation() { return location; }
    public void setLocation(Point location) { this.location = location; }
}
