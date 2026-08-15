package com.valadir.resilience;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Fail-closed when Redis cannot answer: the attempt counter, the blacklist and the refresh store are
 * all unreadable, and none of them may be treated as "nothing found".
 */
class RedisOutageIT extends AbstractResilienceIT {

    private static final String EMAIL = "bruce@wayne.com";
    private static final String PASSWORD = "Gotham#Kn1ght!2024";

    @Test
    void login_redisDown_deniesWithOpaqueInfrastructureError() {

        registerAndActivate(EMAIL, PASSWORD);

        pauseRedis();

        // The attempt counter is the first Redis touch of a login. Answering "no attempts recorded"
        // would hand unlimited credential stuffing to whoever can degrade Redis.
        assertOpaqueInfrastructureFailure(login(EMAIL, PASSWORD));
    }

    // The denial must be temporary. A fail-closed policy that stays closed after the dependency comes
    // back is an outage of its own, and it is also where a circuit stuck open would show up.
    @Test
    void login_afterRedisComesBack_succeedsAgain() {

        registerAndActivate(EMAIL, PASSWORD);

        pauseRedis();

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value());

        resumeRedis();

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }
}
