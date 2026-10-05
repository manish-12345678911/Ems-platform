package com.h8.ems.dispatch.repository;

import com.h8.ems.common.model.UnitStatus;
import com.h8.ems.dispatch.model.AmbulanceUnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AmbulanceUnitRepository extends JpaRepository<AmbulanceUnitEntity, UUID> {

    Optional<AmbulanceUnitEntity> findByCallSign(String callSign);

    List<AmbulanceUnitEntity> findByIdInAndStatus(Collection<UUID> ids, UnitStatus status);

    List<AmbulanceUnitEntity> findByStatus(UnitStatus status);

    /**
     * Atomic conditional update (Hard Rule #3).
     * Modifies status ONLY if current status matches expectedStatus.
     * Prevents read-then-write race conditions.
     */
    @Modifying
    @Query("UPDATE AmbulanceUnitEntity u SET u.status = :newStatus WHERE u.id = :unitId AND u.status = :expectedStatus")
    int updateStatusIfMatches(@Param("unitId") UUID unitId,
                              @Param("newStatus") UnitStatus newStatus,
                              @Param("expectedStatus") UnitStatus expectedStatus);

    default int reserveIfAvailable(UUID unitId) {
        return updateStatusIfMatches(unitId, UnitStatus.DISPATCHED, UnitStatus.AVAILABLE);
    }

    default int freeUnit(UUID unitId) {
        return updateStatusIfMatches(unitId, UnitStatus.AVAILABLE, UnitStatus.DISPATCHED);
    }

    @Modifying
    @Query(value = "UPDATE dispatch.ambulance_unit SET position = public.ST_SetSRID(public.ST_MakePoint(:lon, :lat), 4326)::public.geography, position_at = :now WHERE id = :unitId", nativeQuery = true)
    int updatePosition(@Param("unitId") UUID unitId,
                       @Param("lat") double lat,
                       @Param("lon") double lon,
                       @Param("now") java.time.Instant now);

    @Modifying
    @Query("UPDATE AmbulanceUnitEntity u SET u.status = :status WHERE u.id = :unitId")
    int updateUnitStatus(@Param("unitId") UUID unitId, @Param("status") UnitStatus status);
}
