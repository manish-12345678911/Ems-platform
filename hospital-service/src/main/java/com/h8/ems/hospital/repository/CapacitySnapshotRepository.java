package com.h8.ems.hospital.repository;

import com.h8.ems.hospital.model.CapacitySnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CapacitySnapshotRepository extends JpaRepository<CapacitySnapshotEntity, UUID> {
    Optional<CapacitySnapshotEntity> findFirstByHospitalIdOrderByUpdatedAtDesc(UUID hospitalId);
    List<CapacitySnapshotEntity> findByHospitalIdOrderByUpdatedAtDesc(UUID hospitalId);
}
