package com.valadir.security.redis;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

public final class CircuitGuards {

    private CircuitGuards() {

    }

    /**
     * A guard on a fresh, closed circuit, so every call reaches Redis and no state leaks between tests.
     * The default config needs 100 calls before it can open, well beyond what any test makes.
     */
    public static RedisCircuitGuard buildClosedCircuitGuard() {

        return new RedisCircuitGuard(buildClosedCircuitBreaker());
    }

    /**
     * A fresh, closed circuit breaker per call, so no state leaks between tests. The new registry is
     * what makes it fresh: a shared one caches by name and would hand back the same instance, carrying
     * its call count and state over. The default config needs 100 calls before it can open, well
     * beyond what any test makes.
     */
    public static CircuitBreaker buildClosedCircuitBreaker() {

        return CircuitBreakerRegistry.ofDefaults().circuitBreaker("test");
    }

    public static CircuitBreaker buildOpenCircuitBreaker() {

        var circuitBreaker = buildClosedCircuitBreaker();
        circuitBreaker.transitionToOpenState();
        return circuitBreaker;
    }
}
