
package com.beanstack.notifyx.delivery.service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class DeliveryRetryPolicy {

    private final int maxAttempts;
    private final long initialDelaySeconds;
    private final long maxDelaySeconds;

    public DeliveryRetryPolicy(
        @Value("${notifyx.delivery.retry.max-attempts:5}")
        int maxAttempts,

        @Value("${notifyx.delivery.retry.initial-delay-seconds:30}")
        long initialDelaySeconds,

        @Value("${notifyx.delivery.retry.max-delay-seconds:3600}")
        long maxDelaySeconds
    ) {
        if (maxAttempts < 1 ||
            initialDelaySeconds < 1 ||
            maxDelaySeconds < initialDelaySeconds) {
            throw new IllegalArgumentException(
                "Invalid delivery retry configuration"
            );
        }

        this.maxAttempts = maxAttempts;
        this.initialDelaySeconds = initialDelaySeconds;
        this.maxDelaySeconds = maxDelaySeconds;
    }

    public RetryDecision evaluate(
    int currentAttempt,
    Exception exception
    ) {
        if (currentAttempt >= maxAttempts) {
            return RetryDecision.attemptsExhausted();
        }

        if (!isRetryable(exception)) {
            return RetryDecision.permanentFailure();
        }

        return RetryDecision.retryAfter(
            calculateDelay(currentAttempt)
        );
    }

    private boolean isRetryable(Exception exception) {

        if (exception instanceof IllegalArgumentException ||
            exception instanceof IllegalStateException) {
            return false;
        }

        if (exception instanceof RestClientResponseException responseException) {
            int status = responseException.getStatusCode().value();

            return status == 408 ||
                status == 429 ||
                status >= 500;
        }

        if (exception instanceof ResourceAccessException) {
            return true;
        }

        return true;
    }

    private Duration calculateDelay(int currentAttempt) {

        int exponent = Math.min(currentAttempt - 1, 30);

        long exponentialDelay = Math.min(
            maxDelaySeconds,
            initialDelaySeconds * (1L << exponent)
        );

        long minimum = Math.max(1, exponentialDelay / 2);
        long maximum = Math.max(
            minimum,
            exponentialDelay + exponentialDelay / 2
        );

        long jitteredDelay = ThreadLocalRandom.current()
            .nextLong(minimum, maximum + 1);

        return Duration.ofSeconds(
            Math.min(jitteredDelay, maxDelaySeconds)
        );
    }

}
