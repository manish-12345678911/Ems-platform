package com.h8.ems.dispatch.repository;

import com.h8.ems.dispatch.model.AssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<AssignmentEntity, UUID> {

    List<AssignmentEntity> findByIncidentIdOrderByDecidedAtDesc(UUID incidentId);

    Optional<AssignmentEntity> findFirstByIncidentIdAndUnitIdOrderByDecidedAtDesc(UUID incidentId, UUID unitId);

    Optional<AssignmentEntity> findFirstByUnitIdAndRejectedFalseOrderByDecidedAtDesc(UUID unitId);
}
