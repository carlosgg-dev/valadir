package com.valadir.security.redis;

import com.valadir.common.exception.InfrastructureException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RedisCircuitGuardTest {

    private static final DataAccessException REDIS_ERROR = new DataAccessException("Redis error") {
    };

    private static final String OPERATION = "token blacklist read failed";

    private final CircuitBreaker circuitBreaker = CircuitBreakerRegistry.ofDefaults().circuitBreaker("test");
    private final RedisCircuitGuard guard = new RedisCircuitGuard(circuitBreaker);

    @Test
    void call_redisAvailable_returnsTheValue() {

        String value = guard.call(OPERATION, () -> "stored");

        assertThat(value).isEqualTo("stored");
    }

    @Test
    void call_redisError_reportsItAsInfrastructureUnavailable() {

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> guard.call(OPERATION, () -> {
                throw REDIS_ERROR;
            }))
            .withCause(REDIS_ERROR)
            .withMessageContaining(OPERATION);
    }

    @Test
    void call_openCircuit_reportsItWithoutAttemptingTheCall() {

        var attempts = new AtomicInteger();
        circuitBreaker.transitionToOpenState();

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> guard.call(OPERATION, attempts::incrementAndGet))
            .withCauseInstanceOf(CallNotPermittedException.class);

        assertThat(attempts).hasValue(0);
    }

    @Test
    void call_repeatedRedisErrors_opensTheCircuit() {

        int callsBeforeOpening = 2;
        var config = CircuitBreakerConfig.custom()
            .slidingWindowSize(callsBeforeOpening)
            .minimumNumberOfCalls(callsBeforeOpening)
            .failureRateThreshold(50)
            .build();

        var failingBreaker = CircuitBreakerRegistry.of(config).circuitBreaker("test");
        var failingGuard = new RedisCircuitGuard(failingBreaker);

        IntStream.range(0, callsBeforeOpening).forEach(call ->
            assertThatExceptionOfType(InfrastructureException.class)
                .isThrownBy(() -> failingGuard.call(OPERATION, () -> {
                    throw REDIS_ERROR;
                }))
        );

        assertThat(failingBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void run_redisAvailable_executesTheCall() {

        var attempts = new AtomicInteger();

        guard.run(OPERATION, attempts::incrementAndGet);

        assertThat(attempts).hasValue(1);
    }

    @Test
    void run_redisError_reportsItAsInfrastructureUnavailable() {

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> guard.run(OPERATION, () -> {
                throw REDIS_ERROR;
            }))
            .withCause(REDIS_ERROR);
    }
}
