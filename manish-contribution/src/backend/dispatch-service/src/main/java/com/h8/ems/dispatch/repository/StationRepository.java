package com.h8.ems.dispatch.repository;

import com.h8.ems.dispatch.model.StationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StationRepository extends JpaRepository<StationEntity, UUID> {
}
