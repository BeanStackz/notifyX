package com.beanstack.notifyx.delivery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class DeliveryRetryPolicyTest {

    private final DeliveryRetryPolicy policy =
        new DeliveryRetryPolicy(5, 30, 3600);

    @Test
    void evaluate_shouldRetryTransientFailureBeforeLimit() {
        RetryDecision decision = policy.evaluate(
            2,
            new RuntimeException("temporary network issue")
        );

        assertTrue(decision.retryable());
        assertFalse(decision.exhausted());
        assertNotNull(decision.delay());
        assertTrue(decision.delay().compareTo(Duration.ZERO) > 0);
    }

    @Test
    void evaluate_shouldNotRetryPermanentFailure() {
        RetryDecision decision = policy.evaluate(
            1,
            new IllegalArgumentException("invalid payload")
        );

        assertFalse(decision.retryable());
        assertFalse(decision.exhausted());
        assertEquals(Duration.ZERO, decision.delay());
    }

    @Test
    void evaluate_shouldMarkAttemptsExhaustedAtMaxLimit() {
        RetryDecision decision = policy.evaluate(
            5,
            new RuntimeException("still failing")
        );

        assertFalse(decision.retryable());
        assertTrue(decision.exhausted());
        assertEquals(Duration.ZERO, decision.delay());
    }
}
