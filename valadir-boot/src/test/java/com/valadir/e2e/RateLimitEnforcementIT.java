package com.valadir.e2e;

import com.valadir.common.error.ErrorCode;
import com.valadir.security.redis.RedisKeySpace;
import com.valadir.test.mother.PasswordMother;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@TestPropertySource(properties = "rate-limit.enabled=true")
class RateLimitEnforcementIT extends AbstractAuthE2EIT {

    private static final String EMAIL = "bruce.wayne@email.com";
    private static final String PASSWORD = PasswordMother.raw().value();

    private static final String LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RESET_HEADER = "X-RateLimit-Reset";

    // Mirror the rules of application.yml. application-test.yml declares no rate-limit block, so
    // these pin the production binding: a rule loosened there must break this class, not slip by.
    private static final int REGISTER_EMAIL_LIMIT = 3;
    private static final Duration REGISTER_EMAIL_WINDOW = Duration.ofHours(1);
    private static final int LOGIN_IP_LIMIT = 10;
    private static final Duration LOGIN_IP_WINDOW = Duration.ofSeconds(60);
    private static final int USER_LIMIT = 100;

    private static final int CONCURRENT_LOGINS = LOGIN_IP_LIMIT + 5;

    @Test
    void register_repeatedForTheSameEmail_isBlockedByTheEmailRuleWithoutLosingTheBody() {

        Response created = register(EMAIL, PASSWORD);

        created.then().statusCode(HttpStatus.CREATED.value());

        // The filter reads the body before the controller does. A 201 with the profile intact is
        // what proves it put the body back: without it registration breaks, and no other E2E runs
        // with the limiter on to notice.
        var user = userJpaRepository.findByAccountId(UUID.fromString(accountIdOf(EMAIL))).orElseThrow();
        assertThat(user.getFullName()).isEqualTo(FULL_NAME);
        assertThat(user.getGivenName()).isEqualTo(GIVEN_NAME);

        // Two rules match this route: the headers must come from the tighter one, EMAIL 3/1h
        assertThat(numericHeader(created, LIMIT_HEADER)).isEqualTo(REGISTER_EMAIL_LIMIT);

        List<Response> upToTheLimit = IntStream.rangeClosed(2, REGISTER_EMAIL_LIMIT)
            .mapToObj(attempt -> register(EMAIL, PASSWORD))
            .toList();

        // Re-registering an account still pending activation replaces it, so these are 201s too
        assertThat(responsesWithStatus(upToTheLimit, HttpStatus.CREATED)).hasSize(REGISTER_EMAIL_LIMIT - 1);

        Response blocked = register(EMAIL, PASSWORD);

        blocked.then()
            .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
            .body("code", equalTo(ErrorCode.RATE_LIMIT_EXCEEDED.getCode()))
            .body("errors", nullValue());

        // Limit 3 names the email rule: the IP one (5/1h) still has budget at the fourth request
        assertThat(numericHeader(blocked, LIMIT_HEADER)).isEqualTo(REGISTER_EMAIL_LIMIT);
        assertThat(numericHeader(blocked, HttpHeaders.RETRY_AFTER)).isBetween(1L, REGISTER_EMAIL_WINDOW.toSeconds());
    }

    @Test
    void register_bodyWithoutEmail_skipsTheEmailRuleAndStillReachesValidation() {

        // Nothing to key the email rule on: it must be skipped, not answered. The missing field is
        // the controller's to report.
        registerWithoutEmail(PASSWORD)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo(ErrorCode.INVALID_FIELD.getCode()));

        assertThat(redisTemplate.keys(RedisKeySpace.forRateLimitEmail("*", "*"))).isEmpty();
    }

    @Test
    void login_burstFromOneIp_allowsTheConfiguredLimitAndBlocksTheNext() {

        registerAndActivate(EMAIL, PASSWORD);

        long beforeFirstLogin = Instant.now().getEpochSecond();
        Response first = login(EMAIL, PASSWORD);
        long afterFirstLogin = Instant.now().getEpochSecond();

        first.then().statusCode(HttpStatus.OK.value());

        assertThat(numericHeader(first, LIMIT_HEADER)).isEqualTo(LOGIN_IP_LIMIT);
        assertThat(numericHeader(first, REMAINING_HEADER)).isEqualTo(LOGIN_IP_LIMIT - 1);

        // Reset says when the window ends, not how long is left: clients wait for reset - now, so
        // publishing the plain window there would resolve to no wait at all.
        assertThat(numericHeader(first, RESET_HEADER))
            .isBetween(beforeFirstLogin + LOGIN_IP_WINDOW.toSeconds(), afterFirstLogin + LOGIN_IP_WINDOW.toSeconds());

        List<Response> upToTheLimit = IntStream.rangeClosed(2, LOGIN_IP_LIMIT)
            .mapToObj(attempt -> login(EMAIL, PASSWORD))
            .toList();

        // The limit itself is still served — blocking starts one request later
        assertThat(responsesWithStatus(upToTheLimit, HttpStatus.OK)).hasSize(LOGIN_IP_LIMIT - 1);
        assertThat(numericHeader(upToTheLimit.getLast(), REMAINING_HEADER)).isZero();

        Response blocked = login(EMAIL, PASSWORD);

        // The filter writes this 429 itself, without the exception handler, so the opaque shape the
        // rest of the API answers with has to hold here on its own.
        blocked.then()
            .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
            .body("code", equalTo(ErrorCode.RATE_LIMIT_EXCEEDED.getCode()))
            .body("errors", nullValue());

        assertThat(blocked.contentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(numericHeader(blocked, REMAINING_HEADER)).isZero();
        assertThat(numericHeader(blocked, HttpHeaders.RETRY_AFTER)).isBetween(1L, LOGIN_IP_WINDOW.toSeconds());

        // The credentials were valid, so a missing session can only mean the block came first
        assertThat(sessionFingerprintsOf(accountIdOf(EMAIL))).hasSize(LOGIN_IP_LIMIT);
    }

    @Test
    void login_concurrentBurstOverTheLimit_blocksExactlyTheExcess() {

        registerAndActivate(EMAIL, PASSWORD);

        List<Response> responses = loginConcurrently(CONCURRENT_LOGINS);

        List<Response> allowed = responsesWithStatus(responses, HttpStatus.OK);
        List<Response> blocked = responsesWithStatus(responses, HttpStatus.TOO_MANY_REQUESTS);

        // Counting and recording happen in one atomic script: a read-then-write limiter would let
        // extra requests through exactly when they arrive together.
        assertThat(allowed).hasSize(LOGIN_IP_LIMIT);
        assertThat(blocked).hasSize(CONCURRENT_LOGINS - LOGIN_IP_LIMIT);

        blocked.forEach(response -> response.then().body("code", equalTo(ErrorCode.RATE_LIMIT_EXCEEDED.getCode())));

        // The same count, read from the side effect
        assertThat(sessionFingerprintsOf(accountIdOf(EMAIL))).hasSize(LOGIN_IP_LIMIT);
    }

    @Test
    void logout_authenticatedRequest_consumesTheUserRuleOfThePrincipal() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String accessToken = accessTokenOf(loggedIn);
        String refreshToken = refreshTokenOf(loggedIn);

        // Anonymous traffic resolves no principal: one shared bucket would let a single caller
        // drain the limit of everybody else.
        assertThat(redisTemplate.keys(RedisKeySpace.forRateLimitUser("*"))).isEmpty();

        Response loggedOut = logout(accessToken, refreshToken);

        loggedOut.then().statusCode(HttpStatus.NO_CONTENT.value());

        // The key is built from the authenticated accountId, so it only appears if the filter runs
        // after authentication. Placed before it, the /api/** rule would find no principal and skip
        // itself: the per-user limit would be gone with no error and no log.
        assertThat(redisTemplate.hasKey(RedisKeySpace.forRateLimitUser(accountIdOf(EMAIL)))).isTrue();
        assertThat(numericHeader(loggedOut, LIMIT_HEADER)).isEqualTo(USER_LIMIT);
    }

    @Test
    void requestOutsideTheApiNamespace_isNotRateLimited() {

        RestAssured.given()
            .when()
            .get("/");

        // No rule matches outside /api. The status is the security config's business; what matters
        // is that nothing was counted, and the base flushed Redis before the test.
        assertThat(redisTemplate.keys("*")).isEmpty();
    }

    private List<Response> loginConcurrently(int attempts) {

        return concurrently(attempts, () -> login(EMAIL, PASSWORD));
    }

    // Read as the number a client would parse
    private long numericHeader(Response response, String name) {

        return Long.parseLong(response.header(name));
    }
}
