package com.valadir.common.ratelimit;

public record RateLimitResult(
    boolean allowed,
    long requestCount,
    int maxRequests,
    long remainingTtl) {

}
