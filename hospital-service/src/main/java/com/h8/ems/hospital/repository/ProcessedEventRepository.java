package com.h8.ems.hospital.repository;

import com.h8.ems.hospital.model.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, ProcessedEventEntity.ProcessedEventId> {
    boolean existsByConsumerAndEventId(String consumer, UUID eventId);
}
