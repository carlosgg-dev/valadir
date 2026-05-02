package com.valadir.common.ratelimit;

import java.time.Duration;

public interface RateLimiter {

    RateLimitResult consume(String key, int maxRequests, Duration window);
}
