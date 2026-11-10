package com.beanstack.notifyx.ratelimit.service.impl;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.beanstack.notifyx.ratelimit.algorithm.TokenBucketScript;
import com.beanstack.notifyx.ratelimit.config.RateLimitProperties;
import com.beanstack.notifyx.ratelimit.model.RateLimitResult;
import com.beanstack.notifyx.ratelimit.service.RateLimitService;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class RateLimitServiceImpl implements RateLimitService {

    private final RateLimitProperties rateLimitProperties;
    private final StringRedisTemplate redisTemplate;
    private final TokenBucketScript tokenBucketScript;
    @Override
    public RateLimitResult consume(String key) {
        long capacity = rateLimitProperties.getCapacity();
        double refillRate = rateLimitProperties.getRefillTokensPerSecond();
        long ttlMillis = rateLimitProperties.getBucketTtlSeconds() * 1000;   
        
        List<?> result = redisTemplate.execute(
            tokenBucketScript.getScript(),
            List.of(key),
            String.valueOf(capacity),
            String.valueOf(refillRate),
            "1", // Requesting 1 token
            String.valueOf(ttlMillis)
        );
        if(result == null || result.size() != 3) {
            throw new IllegalStateException("Unexpected result from Redis script");
        }
        long allowed = Long.parseLong(result.get(0).toString());
        long remaining = Long.parseLong(result.get(1).toString());
        long retryAfterMillis = Long.parseLong(result.get(2).toString());
        return new RateLimitResult(
            allowed == 1, capacity, remaining, retryAfterMillis
        );
    }
    
}
