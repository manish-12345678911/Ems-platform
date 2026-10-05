package com.h8.ems.hospital.repository;

import com.h8.ems.hospital.model.HospitalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HospitalRepository extends JpaRepository<HospitalEntity, UUID> {
}
