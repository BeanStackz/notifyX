package com.beanstack.notifyx.ratelimit.model;

public record RateLimitResult(
    boolean allowed,
    long limit,
    long remaining,
    long retryAfterMillis
) {
    
}
