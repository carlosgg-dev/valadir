package com.valadir.security.redis;

import com.valadir.common.exception.InfrastructureException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.dao.DataAccessException;

import java.util.function.Supplier;

/**
 * Runs a Redis call through the circuit breaker and reports any failure as an {@link InfrastructureException}.
 *
 * <p>Once the circuit is open the call is not attempted at all, so a request is answered
 * immediately instead of waiting out the Lettuce timeout. The verdict is the same either way —
 * a Redis failure denies the operation — only the wait changes.
 */
public class RedisCircuitGuard {

    private final CircuitBreaker circuitBreaker;

    public RedisCircuitGuard(CircuitBreaker circuitBreaker) {

        this.circuitBreaker = circuitBreaker;
    }

    public <T> T call(String operation, Supplier<T> redisCall) {

        try {
            return circuitBreaker.executeSupplier(redisCall);
        } catch (DataAccessException | CallNotPermittedException e) {
            throw new InfrastructureException("Redis unavailable — " + operation, e);
        }
    }

    public void run(String operation, Runnable redisCall) {

        call(operation, () -> {
            redisCall.run();
            return null;
        });
    }
}
