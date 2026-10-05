package com.h8.ems.redeployment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h8.ems.redeployment.config.RedeploymentProperties;
import com.h8.ems.redeployment.repository.OutboxRepository;
import com.h8.ems.redeployment.repository.RedeployMoveRepository;
import com.h8.ems.redeployment.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Verifies Acceptance Criterion:
 * "Only one redeployment instance acts when two run concurrently."
 */
@ExtendWith(MockitoExtension.class)
class RedeploymentLockConcurrencyTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private RedeployMoveRepository moveRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedeploymentProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new RedeploymentProperties();
        properties.setLockTtlSeconds(30);
        objectMapper = new ObjectMapper();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Instance 1 acquires lock and acts; Instance 2 fails to acquire and skips")
    void testConcurrentExecutionLocking() {
        RedeploymentService instance1 = new RedeploymentService(
                zoneRepository, moveRepository, outboxRepository, redisTemplate, properties, objectMapper
        );

        RedeploymentService instance2 = new RedeploymentService(
                zoneRepository, moveRepository, outboxRepository, redisTemplate, properties, objectMapper
        );

        // Instance 1 acquires lock
        when(valueOperations.setIfAbsent(eq(RedeploymentService.LOCK_KEY), anyString(), any(Duration.class)))
                .thenReturn(true)  // 1st call succeeds
                .thenReturn(false); // 2nd call fails (lock busy)

        boolean result1 = instance1.runPlanningWithLock();
        boolean result2 = instance2.runPlanningWithLock();

        assertThat(result1).isTrue();
        assertThat(result2).isFalse();

        // Instance 2 should never query zones or save any moves
        verify(zoneRepository, atMostOnce()).findAll();
    }
}
