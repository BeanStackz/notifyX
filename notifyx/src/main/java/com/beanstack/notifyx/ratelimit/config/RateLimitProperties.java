package com.beanstack.notifyx.ratelimit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data 
@ConfigurationProperties (prefix = "notifyx.rate-limit")
public class RateLimitProperties {

    private long capacity = 100; // Default capacity

    private double refillTokensPerSecond = 10; // Default refill rate

    private long bucketTtlSeconds = 60; // Default TTL in seconds

    
}
