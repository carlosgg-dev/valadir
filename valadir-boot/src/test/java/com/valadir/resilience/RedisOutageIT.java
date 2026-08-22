package com.valadir.resilience;

import com.valadir.test.mother.PasswordMother;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fail-closed when Redis cannot answer: the attempt counter, the blacklist and the refresh store are
 * all unreadable, and none of them may be treated as "nothing found".
 */
class RedisOutageIT extends AbstractResilienceIT {

    private static final String EMAIL = "bruce@wayne.com";
    private static final String PASSWORD = PasswordMother.raw().value();

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

    @Test
    void refresh_redisDown_deniesWithoutSpendingTheToken() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String refreshToken = refreshTokenOf(loggedIn);
        String accountId = accountIdOf(EMAIL);
        List<String> sessionsBeforeTheOutage = userTokensOf(accountId);

        pauseRedis();

        // Validating the token is the first Redis touch, so the rotation is never reached. Answering
        // "token not found" instead would deny too, but with a 401 telling the client to log in again.
        assertOpaqueInfrastructureFailure(refresh(refreshToken));

        resumeRedis();

        // A rotation is a revoke and an issue in one script: a denial must leave neither half done.
        assertThat(userTokensOf(accountId)).isEqualTo(sessionsBeforeTheOutage);

        // And the token the client still holds is not spent, so the session survives the outage.
        refresh(refreshToken)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    void authenticatedRequest_redisDown_isDeniedBeforeTheController() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String accessToken = accessTokenOf(loggedIn);
        String refreshToken = refreshTokenOf(loggedIn);

        pauseRedis();

        // Logout is only the stand-in: it is the one authenticated route, and the request never
        // reaches it. The blacklist lookup runs in the filter chain, below the controller, so the 503
        // only exists because the outage reaches InfrastructureFailureFilter untouched. Wrapped in a
        // JwtException — what decode() declares — it would come back as an ordinary 401 instead.
        assertOpaqueInfrastructureFailure(logout(accessToken, refreshToken));
    }

    @Test
    void resendActivationCode_redisDown_failsRetryable() {

        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        pauseRedis();

        Response resendResponse = resendActivationCode(EMAIL);
        
        // The only case here where Redis fails on a write: Postgres is alive, so the account lookup
        // passes and the OTP save is what breaks. The endpoint answers 204 on its other two branches,
        // so the 503 marks the account as existing — kept deliberately, see SmtpDegradationIT.
        assertOpaqueInfrastructureFailure(resendResponse);
    }
}
