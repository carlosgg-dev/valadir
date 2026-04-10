package com.valadir.common.ratelimit;

public interface RateLimiter {

    RateLimitResult consume(String key, int maxRequests, int windowSeconds);
}
