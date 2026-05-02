package com.valadir.common.ratelimit;

import java.time.Duration;

public record RateLimitResult(
    boolean allowed,
    long requestCount,
    int maxRequests,
    Duration remainingTtl) {

    public long remaining() {

        return Math.max(0L, maxRequests - requestCount);
    }

    public boolean isMoreRestrictiveThan(RateLimitResult other) {

        return this.remaining() < other.remaining();
    }
}
