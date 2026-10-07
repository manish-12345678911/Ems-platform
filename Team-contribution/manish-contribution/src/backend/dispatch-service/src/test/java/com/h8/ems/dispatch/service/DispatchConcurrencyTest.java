package com.h8.ems.dispatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.h8.ems.common.model.UnitStatus;
import com.h8.ems.common.model.UnitType;
import com.h8.ems.contracts.dto.DispatchRequest;
import com.h8.ems.contracts.dto.DispatchResponse;
import com.h8.ems.dispatch.exception.UnitNotAvailableException;
import com.h8.ems.dispatch.model.AmbulanceUnitEntity;
import com.h8.ems.dispatch.model.AssignmentEntity;
import com.h8.ems.dispatch.model.OutboxEventEntity;
import com.h8.ems.dispatch.repository.AmbulanceUnitRepository;
import com.h8.ems.dispatch.repository.AssignmentRepository;
import com.h8.ems.dispatch.repository.OutboxRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Concurrency test verifying Hard Rule #3:
 * 50 threads attempting to reserve the SAME unit → exactly 1 succeeds.
 * The conditional UPDATE approach guarantees this without read-then-write.
 */
class DispatchConcurrencyTest {

    @Test
    void fiftyConcurrentReservationsProduceExactlyOneWinner() throws InterruptedException {
        UUID unitId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        // Simulate conditional UPDATE: first call returns 1, all subsequent return 0
        AtomicInteger reserveCallCount = new AtomicInteger(0);

        AmbulanceUnitRepository unitRepository = mock(AmbulanceUnitRepository.class);
        when(unitRepository.reserveIfAvailable(unitId)).thenAnswer(invocation -> {
            // Exactly the first caller wins; all others lose.
            int callNum = reserveCallCount.incrementAndGet();
            return callNum == 1 ? 1 : 0;
        });

        AmbulanceUnitEntity mockUnit = new AmbulanceUnitEntity();
        mockUnit.setId(unitId);
        mockUnit.setCallSign("MEDIC-1");
        mockUnit.setType(UnitType.ALS);
        mockUnit.setStatus(UnitStatus.DISPATCHED);
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(mockUnit));

        AssignmentRepository assignmentRepository = mock(AssignmentRepository.class);
        when(assignmentRepository.save(any(AssignmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OutboxRepository outboxRepository = mock(OutboxRepository.class);
        when(outboxRepository.save(any(OutboxEventEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        DispatchExecutionService service = new DispatchExecutionService(
                unitRepository, assignmentRepository, outboxRepository,
                new ObjectMapper().registerModule(new JavaTimeModule())
        );

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Submit 50 concurrent dispatch requests
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await(); // all threads start simultaneously
                    DispatchRequest req = new DispatchRequest(incidentId, unitId, "AUTO");
                    DispatchResponse resp = service.dispatch(req);
                    if (resp != null) {
                        successCount.incrementAndGet();
                    }
                } catch (UnitNotAvailableException e) {
                    failureCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Release all threads at once
        startGate.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        // Verify: exactly 1 winner, 49 losers
        assertEquals(1, successCount.get(), "Exactly one thread should succeed");
        assertEquals(49, failureCount.get(), "Exactly 49 threads should fail with UnitNotAvailableException");
        assertEquals(50, reserveCallCount.get(), "All 50 threads should have attempted reservation");
    }
}
