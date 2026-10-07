package com.h8.ems.dispatch.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a unit assignment to an incident in dispatch.assignment.
 */
@Entity
@Table(name = "assignment", schema = "dispatch")
public class AssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private AmbulanceUnitEntity unit;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ranked_snapshot", columnDefinition = "jsonb")
    private String rankedSnapshot;

    @Column(name = "chosen_by", length = 64)
    private String chosenBy;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt = Instant.now();

    @Column(name = "rejected", nullable = false)
    private boolean rejected = false;

    public AssignmentEntity() {
    }

    public AssignmentEntity(UUID id, UUID incidentId, AmbulanceUnitEntity unit,
                            String rankedSnapshot, String chosenBy, Instant decidedAt, boolean rejected) {
        this.id = id;
        this.incidentId = incidentId;
        this.unit = unit;
        this.rankedSnapshot = rankedSnapshot;
        this.chosenBy = chosenBy;
        this.decidedAt = decidedAt != null ? decidedAt : Instant.now();
        this.rejected = rejected;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }

    public AmbulanceUnitEntity getUnit() { return unit; }
    public void setUnit(AmbulanceUnitEntity unit) { this.unit = unit; }

    public String getRankedSnapshot() { return rankedSnapshot; }
    public void setRankedSnapshot(String rankedSnapshot) { this.rankedSnapshot = rankedSnapshot; }

    public String getChosenBy() { return chosenBy; }
    public void setChosenBy(String chosenBy) { this.chosenBy = chosenBy; }

    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }

    public boolean isRejected() { return rejected; }
    public void setRejected(boolean rejected) { this.rejected = rejected; }
}
