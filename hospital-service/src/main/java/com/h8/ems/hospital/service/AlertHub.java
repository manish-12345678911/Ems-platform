package com.h8.ems.hospital.service;

import com.h8.ems.contracts.dto.PreArrivalAlertDto;
import com.h8.ems.hospital.model.PreArrivalAlertEntity;
import com.h8.ems.hospital.repository.PreArrivalAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-Sent Events (SSE) hub for dispatching pre-arrival trauma alerts to ED dashboards.
 * Implements:
 * - 15-second heartbeat to prevent idle proxy disconnects.
 * - Automatic emitter cleanup on timeout or error.
 * - Ring-buffer message history with Last-Event-ID support for seamless client reconnects (Correction #9).
 */
@Service
@EnableScheduling
public class AlertHub {

    private static final Logger log = LoggerFactory.getLogger(AlertHub.class);
    private static final int BUFFER_SIZE = 100;
    private static final long EMITTER_TIMEOUT = 180_000L; // 3 minutes

    private final PreArrivalAlertRepository alertRepository;
    private final Map<UUID, List<SseEmitter>> hospitalEmitters = new ConcurrentHashMap<>();
    private final List<PreArrivalAlertDto> ringBuffer = new CopyOnWriteArrayList<>();

    public AlertHub(PreArrivalAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /**
     * Subscribes an SSE client (e.g. ED dashboard) to pre-arrival alerts for a specific hospital.
     */
    public SseEmitter subscribe(UUID hospitalId, String lastEventId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);

        hospitalEmitters.computeIfAbsent(hospitalId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(hospitalId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            removeEmitter(hospitalId, emitter);
        });
        emitter.onError(e -> removeEmitter(hospitalId, emitter));

        // Replay missed alerts if Last-Event-ID is provided (Correction #9)
        if (lastEventId != null && !lastEventId.isBlank()) {
            try {
                UUID lastId = UUID.fromString(lastEventId);
                boolean foundLast = false;
                for (PreArrivalAlertDto alert : ringBuffer) {
                    if (foundLast && alert.hospitalId().equals(hospitalId)) {
                        sendAlertToEmitter(emitter, alert);
                    }
                    if (alert.alertId().equals(lastId)) {
                        foundLast = true;
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }

        // Send initial connect ping
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data("Connected to H8 ED AlertHub for hospital " + hospitalId));
        } catch (IOException e) {
            removeEmitter(hospitalId, emitter);
        }

        return emitter;
    }

    /**
     * Publishes a pre-arrival alert to all registered dashboards for this hospital.
     */
    public void publishAlert(PreArrivalAlertDto alert) {
        // Persist to DB
        PreArrivalAlertEntity entity = new PreArrivalAlertEntity(
                alert.alertId() != null ? alert.alertId() : UUID.randomUUID(),
                alert.incidentId(),
                alert.hospitalId(),
                alert.etaSeconds(),
                alert.sentAt() != null ? alert.sentAt() : Instant.now(),
                null
        );
        alertRepository.save(entity);

        // Add to ring buffer
        if (ringBuffer.size() >= BUFFER_SIZE) {
            ringBuffer.remove(0);
        }
        ringBuffer.add(alert);

        // Broadcast to hospital's connected emitters
        List<SseEmitter> emitters = hospitalEmitters.get(alert.hospitalId());
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                sendAlertToEmitter(emitter, alert);
            }
        }
    }

    private void sendAlertToEmitter(SseEmitter emitter, PreArrivalAlertDto alert) {
        try {
            emitter.send(SseEmitter.event()
                    .id(alert.alertId().toString())
                    .name("PRE_ARRIVAL_ALERT")
                    .data(alert));
        } catch (IOException e) {
            removeEmitter(alert.hospitalId(), emitter);
        }
    }

    private void removeEmitter(UUID hospitalId, SseEmitter emitter) {
        List<SseEmitter> list = hospitalEmitters.get(hospitalId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                hospitalEmitters.remove(hospitalId);
            }
        }
    }

    /**
     * Periodic 15-second heartbeat ping to prevent connection timeout.
     */
    @Scheduled(fixedRate = 15000)
    public void sendHeartbeats() {
        for (Map.Entry<UUID, List<SseEmitter>> entry : hospitalEmitters.entrySet()) {
            UUID hospitalId = entry.getKey();
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().name("HEARTBEAT").data("ping"));
                } catch (IOException e) {
                    removeEmitter(hospitalId, emitter);
                }
            }
        }
    }

    public int getActiveSubscriptionCount(UUID hospitalId) {
        List<SseEmitter> list = hospitalEmitters.get(hospitalId);
        return list != null ? list.size() : 0;
    }
}
