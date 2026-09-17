package com.valadir.common.ratelimit;

import java.time.Duration;

public interface RateLimiter {

    RateLimitResult consume(RateLimitSubject subject, int maxRequests, Duration window);
}
