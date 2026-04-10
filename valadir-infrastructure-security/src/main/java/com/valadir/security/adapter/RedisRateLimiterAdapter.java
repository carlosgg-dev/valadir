package com.valadir.security.adapter;

import com.valadir.common.ratelimit.RateLimitResult;
import com.valadir.common.ratelimit.RateLimiter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("rawtypes")
@Component
class RedisRateLimiterAdapter implements RateLimiter {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<List> rateLimitScript;

    RedisRateLimiterAdapter(final RedisTemplate<String, String> redisTemplate) {

        this.redisTemplate = redisTemplate;
        this.rateLimitScript = RedisScript.of(new ClassPathResource("scripts/rate_limit.lua"), List.class);
    }

    @Override
    public RateLimitResult consume(final String key, final int maxRequests, final int windowSeconds) {

        final List<?> result = Objects.requireNonNull(
            redisTemplate.execute(rateLimitScript, List.of(key), String.valueOf(maxRequests), String.valueOf(windowSeconds)),
            "Rate limit script returned no result for key: " + key
        );

        final long requestCount = (Long) result.get(0);
        final long remainingTtl = (Long) result.get(1);

        return new RateLimitResult(requestCount <= maxRequests, requestCount, maxRequests, remainingTtl);
    }
}
