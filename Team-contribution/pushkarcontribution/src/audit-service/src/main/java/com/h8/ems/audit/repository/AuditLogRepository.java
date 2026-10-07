package com.h8.ems.audit.repository;

import com.h8.ems.audit.model.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    Optional<AuditLogEntity> findTopByOrderBySeqDesc();

    List<AuditLogEntity> findAllByOrderBySeqAsc();

    List<AuditLogEntity> findAllByOrderBySeqDesc();

    Optional<AuditLogEntity> findByEventId(UUID eventId);

    List<AuditLogEntity> findByKindOrderBySeqDesc(String kind);

    @Query(value = "SELECT * FROM audit.audit_log WHERE payload ->> 'incidentId' = :incidentId ORDER BY seq DESC", nativeQuery = true)
    List<AuditLogEntity> findByIncidentIdInPayload(@Param("incidentId") String incidentId);

    Page<AuditLogEntity> findAllByOrderBySeqDesc(Pageable pageable);
}
