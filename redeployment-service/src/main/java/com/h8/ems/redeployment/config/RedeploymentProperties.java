package com.h8.ems.redeployment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "h8.redeployment")
public class RedeploymentProperties {

    private int lockTtlSeconds = 30;
    private int maxMoves = 3;
    private double minCoverageGain = 0.01;
    private int cooldownMinutes = 10;
    private double coverageRadiusKm = 5.0;

    public int getLockTtlSeconds() {
        return lockTtlSeconds;
    }

    public void setLockTtlSeconds(int lockTtlSeconds) {
        this.lockTtlSeconds = lockTtlSeconds;
    }

    public int getMaxMoves() {
        return maxMoves;
    }

    public void setMaxMoves(int maxMoves) {
        this.maxMoves = maxMoves;
    }

    public double getMinCoverageGain() {
        return minCoverageGain;
    }

    public void setMinCoverageGain(double minCoverageGain) {
        this.minCoverageGain = minCoverageGain;
    }

    public int getCooldownMinutes() {
        return cooldownMinutes;
    }

    public void setCooldownMinutes(int cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public double getCoverageRadiusKm() {
        return coverageRadiusKm;
    }

    public void setCoverageRadiusKm(double coverageRadiusKm) {
        this.coverageRadiusKm = coverageRadiusKm;
    }
}
