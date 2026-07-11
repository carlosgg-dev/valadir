package com.valadir.e2e;

import com.valadir.security.redis.RedisKeySpace;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * Temporary Phase-0 scaffolding: proves the E2E wiring (RANDOM_PORT + REST Assured +
 * Testcontainers via {@code @ServiceConnection}) end-to-end. Deleted in Phase 1 —
 * {@code LoginFlowIT} exercises the same path.
 */
@TestPropertySource(properties = "rate-limit.enabled=false")
class E2EWiringSmokeIT extends AbstractAuthE2EIT {

    @Test
    void login_unknownCredentials_returnsOpaque401AndRecordsFailedAttemptInRedis() {

        var email = "smoke@valadir.test";

        login(email, "WrongP@ssword123")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("SEC-001"))
            .body("errors", nullValue());

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forLoginAttempts(email)))
            .isEqualTo("1");
    }
}
