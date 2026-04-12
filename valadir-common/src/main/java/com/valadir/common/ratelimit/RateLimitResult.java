package com.valadir.common.ratelimit;

public record RateLimitResult(
    boolean allowed,
    long requestCount,
    int maxRequests,
    long remainingTtl) {

    public long remaining() {

        return Math.max(0L, maxRequests - requestCount);
    }
}
