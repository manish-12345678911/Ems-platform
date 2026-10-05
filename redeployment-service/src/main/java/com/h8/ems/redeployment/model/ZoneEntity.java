package com.h8.ems.redeployment.model;

import com.h8.ems.common.model.GeoPoint;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "zone", schema = "redeploy")
public class ZoneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "centroid", nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point centroid;

    @Column(name = "demand_per_hour", nullable = false)
    private double demandPerHour = 0.0;

    public ZoneEntity() {
    }

    public ZoneEntity(Integer id, Point centroid, double demandPerHour) {
        this.id = id;
        this.centroid = centroid;
        this.demandPerHour = demandPerHour;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Point getCentroid() {
        return centroid;
    }

    public void setCentroid(Point centroid) {
        this.centroid = centroid;
    }

    public double getDemandPerHour() {
        return demandPerHour;
    }

    public void setDemandPerHour(double demandPerHour) {
        this.demandPerHour = demandPerHour;
    }

    public GeoPoint toGeoPoint() {
        if (centroid == null) {
            return new GeoPoint(0.0, 0.0);
        }
        return new GeoPoint(centroid.getY(), centroid.getX());
    }
}
