package com.valadir.security.redis;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

public final class CircuitGuards {

    private CircuitGuards() {

    }

    /**
     * A guard on a fresh, closed circuit, so every call reaches Redis and no state leaks between tests.
     * The default config needs 100 calls before it can open, well beyond what any test makes.
     */
    public static RedisCircuitGuard buildClosedCircuitGuard() {

        return new RedisCircuitGuard(CircuitBreakerRegistry.ofDefaults().circuitBreaker("test"));
    }
}
