package com.valadir.resilience;

import com.valadir.test.mother.PasswordMother;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fail-closed when the rate limiter cannot be evaluated: a limit that cannot be read is not a limit,
 * so the request is refused before anything else runs.
 *
 * <p>The limiter is off on the resilience base, as it is on the flow E2E base — a burst from
 * localhost would answer 429 before the case under test ever ran. This is the one class that needs
 * it on, so it pays for its own cached context over the same isolated containers.
 */
@TestPropertySource(properties = "rate-limit.enabled=true")
class RateLimiterOutageIT extends AbstractResilienceIT {

    private static final String EMAIL = "bruce@wayne.com";
    private static final String PASSWORD = PasswordMother.raw().value();

    private static final String LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RESET_HEADER = "X-RateLimit-Reset";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    @Test
    void register_rateLimiterDown_deniesWithoutRateLimitHeaders() {

        pauseRedis();

        Response response = register(EMAIL, PASSWORD);

        assertOpaqueInfrastructureFailure(response);

        // A 503 must not be dressed as a 429: the client is not being throttled, and no limiter state
        // crosses the boundary. The denial is raised in the filter chain, so it only reads as
        // INFRASTRUCTURE_UNAVAILABLE because InfrastructureFailureFilter wraps the limiter.
        assertThat(response.header(LIMIT_HEADER)).isNull();
        assertThat(response.header(REMAINING_HEADER)).isNull();
        assertThat(response.header(RESET_HEADER)).isNull();
        assertThat(response.header(RETRY_AFTER_HEADER)).isNull();
    }

    @Test
    void register_rateLimiterDown_deniesBeforeTheAccountIsPersisted() {

        pauseRedis();

        Response response = register(EMAIL, PASSWORD);

        assertOpaqueInfrastructureFailure(response);

        // Fail-open and fail-closed answer the same 503 here, so the status proves nothing: the
        // INSERT commits before the activation OTP reaches Redis, so a limiter that let the request
        // through would also blow up downstream — but would leave the account behind. Only Redis is
        // paused, so Postgres can be read.
        assertThat(accountJpaRepository.findByEmail(EMAIL)).isEmpty();

        resumeRedis();

        // The denial is temporary: nothing latched the endpoint shut once Redis answers again.
        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());
    }
}
