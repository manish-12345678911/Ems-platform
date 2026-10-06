package com.h8.ems.audit.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit log entry with cryptographic hash chain (SHA-256).
 */
@Entity
@Table(name = "audit_log", schema = "audit")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq")
    private Long seq;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "kind", nullable = false, length = 32)
    private String kind;

    @Column(name = "actor", length = 64)
    private String actor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "prev_hash", length = 64)
    private String prevHash;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "hash", nullable = false, length = 64)
    private String hash;

    @Column(name = "at", nullable = false)
    private Instant at = Instant.now();

    public AuditLogEntity() {
    }

    public AuditLogEntity(UUID eventId, String kind, String actor, String payload,
                          String prevHash, String hash, Instant at) {
        this.eventId = eventId;
        this.kind = kind;
        this.actor = actor;
        this.payload = payload;
        this.prevHash = prevHash;
        this.hash = hash;
        this.at = at;
    }

    public Long getSeq() {
        return seq;
    }

    public void setSeq(Long seq) {
        this.seq = seq;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public void setPrevHash(String prevHash) {
        this.prevHash = prevHash;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Instant getAt() {
        return at;
    }

    public void setAt(Instant at) {
        this.at = at;
    }
}
