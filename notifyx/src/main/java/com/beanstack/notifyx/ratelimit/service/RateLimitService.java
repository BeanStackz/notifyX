package com.beanstack.notifyx.ratelimit.service;

import com.beanstack.notifyx.ratelimit.model.RateLimitResult;

public interface RateLimitService {

    RateLimitResult consume(String key);
    
}
