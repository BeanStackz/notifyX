
package com.beanstack.notifyx.delivery.service;

import java.time.Duration;

public record RetryDecision(
    boolean retryable,
    boolean exhausted,
    Duration delay
) {

    public static RetryDecision retryAfter(Duration delay) {
        return new RetryDecision(true, false, delay);
    }

    public static RetryDecision permanentFailure() {
        return new RetryDecision(false, false, Duration.ZERO);
    }

    public static RetryDecision attemptsExhausted() {
        return new RetryDecision(false, true, Duration.ZERO);
    }
}
