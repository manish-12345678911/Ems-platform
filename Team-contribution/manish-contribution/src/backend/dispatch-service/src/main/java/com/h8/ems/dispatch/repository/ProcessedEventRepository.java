package com.h8.ems.dispatch.repository;

import com.h8.ems.dispatch.model.ProcessedEventEntity;
import com.h8.ems.dispatch.model.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, ProcessedEventId> {

    boolean existsByConsumerAndEventId(String consumer, UUID eventId);
}
