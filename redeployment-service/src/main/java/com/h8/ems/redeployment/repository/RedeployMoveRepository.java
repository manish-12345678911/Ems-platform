package com.h8.ems.redeployment.repository;

import com.h8.ems.redeployment.model.RedeployMoveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RedeployMoveRepository extends JpaRepository<RedeployMoveEntity, UUID> {

    List<RedeployMoveEntity> findByAcceptedFalseOrderByAtDesc();

    List<RedeployMoveEntity> findAllByOrderByAtDesc();

    List<RedeployMoveEntity> findByUnitIdOrderByAtDesc(UUID unitId);

    Optional<RedeployMoveEntity> findTopByUnitIdOrderByAtDesc(UUID unitId);
}
