package com.valadir.resilience;

import com.valadir.e2e.AuthE2ESupport;
import com.valadir.e2e.support.CaptchaVerifierTestConfig;
import com.valadir.e2e.support.NotifierCapturingTestConfig;
import com.valadir.resilience.support.ContainerFailure;
import com.valadir.resilience.support.IsolatedPostgresContainerConfig;
import com.valadir.resilience.support.IsolatedRedisContainerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base for the resilience suite: the same vocabulary as the flow E2E, over containers this suite
 * owns and may therefore pause.
 *
 * <p>An outage is injected with {@code docker pause}, so the dependency hangs rather than refusing
 * cleanly — the case the 2s Redis and 5s Postgres deadlines exist to survive. Whatever a test pauses
 * is given back afterwards, and the Redis circuit is reset, so no test inherits an open circuit from
 * the one before it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "rate-limit.enabled=false")
@Import({
    IsolatedPostgresContainerConfig.class,
    IsolatedRedisContainerConfig.class,
    NotifierCapturingTestConfig.class,
    CaptchaVerifierTestConfig.class
})
public abstract class AbstractResilienceIT extends AuthE2ESupport {

    protected static final String INFRASTRUCTURE_UNAVAILABLE_CODE = "INFRA-001";

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @AfterEach
    void restoreInfrastructure() {

        resumeRedis();
        resumePostgres();

        // The verdict of an open circuit is the same as a real failure, but its latency is not, and a
        // recovery case would measure the breaker's half-open window instead of the dependency's.
        circuitBreakerRegistry.circuitBreaker("redis").reset();
    }

    protected void pauseRedis() {

        ContainerFailure.pause(IsolatedRedisContainerConfig.container());
    }

    protected void resumeRedis() {

        ContainerFailure.resume(IsolatedRedisContainerConfig.container(), this::pingRedis);
    }

    protected void pausePostgres() {

        ContainerFailure.pause(IsolatedPostgresContainerConfig.container());
    }

    protected void resumePostgres() {

        ContainerFailure.resume(IsolatedPostgresContainerConfig.container(), accountJpaRepository::count);
    }

    /**
     * Nothing but the error code crosses the boundary. An outage must be externally
     * indistinguishable from any other 503 — no stack trace, no host, no driver wording.
     */
    protected void assertOpaqueInfrastructureFailure(Response response) {

        response.then().statusCode(HttpStatus.SERVICE_UNAVAILABLE.value());

        assertThat(response.jsonPath().getMap("$"))
            .containsExactly(Map.entry("code", INFRASTRUCTURE_UNAVAILABLE_CODE));
    }

    private void pingRedis() {

        var factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());

        try (var connection = factory.getConnection()) {
            connection.ping();
        }
    }
}
