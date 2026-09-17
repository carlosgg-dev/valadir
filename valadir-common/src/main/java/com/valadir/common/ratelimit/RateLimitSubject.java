package com.valadir.common.ratelimit;

/**
 * What a bucket is counted against: the strategy that picked the value, the route the rule
 * guards, and the value itself. How the three are spelled into a key belongs to the limiter,
 * so a caller never has to know which technology backs it.
 */
public record RateLimitSubject(
    RateLimitStrategy strategy,
    String scope,
    String value) {

}
