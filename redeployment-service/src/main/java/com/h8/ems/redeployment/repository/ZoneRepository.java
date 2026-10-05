package com.h8.ems.redeployment.repository;

import com.h8.ems.redeployment.model.ZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZoneRepository extends JpaRepository<ZoneEntity, Integer> {
}
