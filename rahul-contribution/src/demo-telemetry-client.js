/**
 * H8 EMS — Paramedic GPS Telemetry Streaming Client
 * Author: Rahul (Crew Mobile & Telemetry Engineer)
 * 
 * Purpose:
 * - Collects real or simulated vehicle GPS coordinates
 * - Calculates instantaneous velocity (km/h) and heading bearing (degrees)
 * - Broadcasts telemetry packets to the central cloud platform at 1Hz frequency
 */

class AmbulanceTelemetryStreamer {
    constructor(unitId = 'AMB-02', initialLat = 26.9239, initialLon = 75.8267) {
        this.unitId = unitId;
        this.lat = initialLat;
        this.lon = initialLon;
        this.speedKmH = 0;
        this.bearing = 90;
        this.isStreaming = false;
        this.timer = null;
    }

    /**
     * Computes compass heading between two geographical coordinates
     */
    calculateBearing(startLat, startLng, destLat, destLng) {
        const toRad = deg => (deg * Math.PI) / 180;
        const toDeg = rad => (rad * 180) / Math.PI;

        const y = Math.sin(toRad(destLng - startLng)) * Math.cos(toRad(destLat));
        const x = Math.cos(toRad(startLat)) * Math.sin(toRad(destLat)) -
                  Math.sin(toRad(startLat)) * Math.cos(toRad(destLat)) * Math.cos(toRad(destLng - startLng));
        let brng = Math.atan2(y, x);
        brng = toDeg(brng);
        return (brng + 360) % 360;
    }

    /**
     * Generates a realistic GPS waypoint step simulating ambulance travel
     */
    generateNextWaypoint() {
        // Move slightly towards SMS Hospital (Jaipur: 26.8918, 75.8156)
        const dLat = (26.8918 - this.lat) * 0.05 + (Math.random() - 0.5) * 0.0005;
        const dLon = (75.8156 - this.lon) * 0.05 + (Math.random() - 0.5) * 0.0005;

        const prevLat = this.lat;
        const prevLon = this.lon;

        this.lat += dLat;
        this.lon += dLon;
        this.bearing = Math.round(this.calculateBearing(prevLat, prevLon, this.lat, this.lon));
        this.speedKmH = Math.round(45 + Math.random() * 20); // 45 - 65 km/h emergency speed

        return {
            unitId: this.unitId,
            latitude: parseFloat(this.lat.toFixed(6)),
            longitude: parseFloat(this.lon.toFixed(6)),
            speedKmH: this.speedKmH,
            bearingDegrees: this.bearing,
            accuracyMeters: 4.2,
            timestamp: new Date().toISOString()
        };
    }

    /**
     * Starts the 1000ms telemetry broadcast loop
     */
    startStreaming(callback) {
        this.isStreaming = true;
        console.log(`[TELEMETRY] Ambulance unit ${this.unitId} started high-frequency GPS satellite broadcast.`);

        this.timer = setInterval(() => {
            if (!this.isStreaming) return;
            const packet = this.generateNextWaypoint();
            if (callback) callback(packet);
        }, 1000);
    }

    stopStreaming() {
        this.isStreaming = false;
        if (this.timer) clearInterval(this.timer);
        console.log(`[TELEMETRY] Ambulance unit ${this.unitId} GPS telemetry stopped.`);
    }
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = AmbulanceTelemetryStreamer;
}
