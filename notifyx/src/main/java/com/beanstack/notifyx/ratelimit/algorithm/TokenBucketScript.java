package com.beanstack.notifyx.ratelimit.algorithm;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component 
public class TokenBucketScript {

    private final DefaultRedisScript<List> script;

    public TokenBucketScript() {

        String luaScript = """
                local key = KEYS[1]

                local capacity = tonumber(ARGV[1])
                local refillRate = tonumber(ARGV[2])
                local requestedTokens = tonumber(ARGV[3])
                local ttl = tonumber(ARGV[4])

                local time = redis.call('TIME')

                local now = tonumber(time[1]) * 1000
                    + math.floor(tonumber(time[2]) / 1000)

                local bucket = redis.call(
                    'HMGET',
                    key,
                    'tokens',
                    'lastRefill'
                )

                local tokens = tonumber(bucket[1])
                local lastRefill = tonumber(bucket[2])

                if tokens == nil then
                    tokens = capacity
                    lastRefill = now
                end

                local elapsed = math.max(0, now - lastRefill)

                tokens = math.min(
                    capacity,
                    tokens + elapsed * refillRate / 1000
                )

                local allowed = 0
                local retryAfter = 0

                if tokens >= requestedTokens then

                    tokens = tokens - requestedTokens
                    allowed = 1

                else

                    retryAfter = math.ceil(
                        (requestedTokens - tokens) * 1000 / refillRate
                    )

                end

                redis.call(
                    'HSET',
                    key,
                    'tokens',
                    tokens,
                    'lastRefill',
                    now
                )

                redis.call('PEXPIRE', key, ttl)

                return {
                    allowed,
                    math.floor(tokens),
                    retryAfter
                }
                """;

        this.script = new DefaultRedisScript<>();
        this.script.setScriptText(luaScript);
        this.script.setResultType(List.class);
    }

    public DefaultRedisScript<List> getScript() {
        return script;
    }
    
}
