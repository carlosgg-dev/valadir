package com.valadir.e2e;

import com.valadir.security.redis.RedisKeySpace;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

class RefreshTokenIT extends AbstractAuthE2EIT {

    private static final String EMAIL = "bruce.wayne@email.com";
    private static final String BYSTANDER_EMAIL = "clark.kent@email.com";
    private static final String PASSWORD = "SecureP@ss123";

    private static final String INVALID_TOKEN_CODE = "SEC-003";
    private static final String INVALID_FIELD_CODE = "VAL-001";

    // Mirrors auth.jwt.refresh-token-ttl. application-test.yml redeclares it with the production
    // value, so this pins the test binding — an override there would silently retarget the assertion.
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    @Test
    void refresh_validToken_rotatesTokenAndReplacesItInRedis() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String oldAccessToken = accessTokenOf(loggedIn);
        String oldRefreshToken = refreshTokenOf(loggedIn);

        Response refreshed = refresh(oldRefreshToken);

        refreshed.then()
            .statusCode(HttpStatus.OK.value())
            .body("accessToken", notNullValue())
            .body("refreshToken", notNullValue());

        String newAccessToken = accessTokenOf(refreshed);
        String newRefreshToken = refreshTokenOf(refreshed);

        // Rotation means new material on both tokens: reissuing the same access token would keep
        // a stolen one alive past its refresh, defeating the point of the rotation.
        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);
        assertThat(newAccessToken).isNotEqualTo(oldAccessToken);

        String oldTokenKey = RedisKeySpace.forRefreshToken(oldRefreshToken);
        assertThat(redisTemplate.opsForValue().get(oldTokenKey)).isNull();

        String accountId = accountIdOf(EMAIL);
        String newTokenKey = RedisKeySpace.forRefreshToken(newRefreshToken);

        assertThat(redisTemplate.opsForValue().get(newTokenKey)).isEqualTo(accountId);
        assertThat(redisTemplate.getExpire(newTokenKey))
            .isBetween(REFRESH_TOKEN_TTL.minusMinutes(1).toSeconds(), REFRESH_TOKEN_TTL.toSeconds());

        // Still exactly one session: the rotation swapped the member instead of piling up a second
        // one, so the set does not leak an entry per refresh over the token's 7-day life.
        assertThat(userTokensOf(accountId)).containsExactly(newRefreshToken);
    }

    @ParameterizedTest
    @MethodSource("blankRefreshTokens")
    void refresh_blankToken_returns400AndLeavesSessionIntact(String blankRefreshToken) {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String refreshToken = refreshTokenOf(loggedIn);

        refresh(blankRefreshToken)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo(INVALID_FIELD_CODE))
            .body("errors.field", hasItem("refreshToken"));

        String accountId = accountIdOf(EMAIL);

        // Bean Validation rejects the request before the use case runs, so nothing was rotated.
        assertThat(userTokensOf(accountId)).containsExactly(refreshToken);
    }

    @Test
    void refresh_rotatedToken_isRejectedWithOpaque401() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String oldRefreshToken = refreshTokenOf(loggedIn);

        Response refreshed = refresh(oldRefreshToken);
        String newRefreshToken = refreshTokenOf(refreshed);

        refresh(oldRefreshToken)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_TOKEN_CODE))
            .body("errors", nullValue());

        String accountId = accountIdOf(EMAIL);

        // The replay must not take the live session down with it: rejecting a spent token is not a
        // reason to revoke the one the legitimate user is holding.
        assertThat(userTokensOf(accountId)).containsExactly(newRefreshToken);
    }

    @Test
    void refresh_oneAccountWithTwoSessions_leavesTheOtherSessionUntouched() {

        registerAndActivate(EMAIL, PASSWORD);

        Response refreshingDeviceLogin = login(EMAIL, PASSWORD);
        String refreshingDeviceToken = refreshTokenOf(refreshingDeviceLogin);

        // The bystander device never acts: it is only here to prove the rotation leaves it alone.
        Response bystanderDeviceLogin = login(EMAIL, PASSWORD);
        String bystanderDeviceToken = refreshTokenOf(bystanderDeviceLogin);

        Response refreshed = refresh(refreshingDeviceToken);
        String rotatedToken = refreshTokenOf(refreshed);

        String accountId = accountIdOf(EMAIL);

        assertThat(userTokensOf(accountId))
            .containsExactlyInAnyOrder(rotatedToken, bystanderDeviceToken);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(bystanderDeviceToken)))
            .isNotNull();
    }

    @Test
    void refresh_twoAccountsWithLiveSessions_rotatesOnlyTheOwningAccount() {

        registerAndActivate(EMAIL, PASSWORD);
        registerAndActivate(BYSTANDER_EMAIL, PASSWORD);

        Response rotatingAccountLogin = login(EMAIL, PASSWORD);
        String rotatingAccountToken = refreshTokenOf(rotatingAccountLogin);

        // The bystander account never refreshes: its session is the state the rotation must not reach.
        Response bystanderAccountLogin = login(BYSTANDER_EMAIL, PASSWORD);
        String bystanderAccountToken = refreshTokenOf(bystanderAccountLogin);

        Response refreshed = refresh(rotatingAccountToken);
        String rotatedToken = refreshTokenOf(refreshed);

        String rotatingAccountId = accountIdOf(EMAIL);
        String bystanderAccountId = accountIdOf(BYSTANDER_EMAIL);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(rotatedToken)))
            .isEqualTo(rotatingAccountId);
        assertThat(userTokensOf(rotatingAccountId)).containsExactly(rotatedToken);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(bystanderAccountToken)))
            .isEqualTo(bystanderAccountId);
        assertThat(userTokensOf(bystanderAccountId)).containsExactly(bystanderAccountToken);
    }

    @Test
    void refresh_concurrentUseOfSameToken_grantsExactlyOneRotation() throws Exception {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String refreshToken = refreshTokenOf(loggedIn);

        List<Response> responses = refreshConcurrently(refreshToken);

        List<Response> accepted = responses.stream()
            .filter(response -> response.statusCode() == HttpStatus.OK.value())
            .toList();

        List<Response> rejected = responses.stream()
            .filter(response -> response.statusCode() != HttpStatus.OK.value())
            .toList();

        assertThat(accepted).hasSize(1);
        assertThat(rejected).hasSize(1);

        rejected.getFirst().then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_TOKEN_CODE));

        String winningToken = refreshTokenOf(accepted.getFirst());
        String accountId = accountIdOf(EMAIL);

        assertThat(userTokensOf(accountId)).containsExactly(winningToken);
    }

    // Both threads wait on the latch and are released together, so the two requests reach Redis in
    // the same window. Just submitting them would let the first finish before the second starts —
    // no race at all, only the spent-token path another case already covers.
    private List<Response> refreshConcurrently(String refreshToken) throws Exception {

        var startLine = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {

            List<Future<Response>> pending = List.of(
                executor.submit(() -> awaitAndRefresh(startLine, refreshToken)),
                executor.submit(() -> awaitAndRefresh(startLine, refreshToken))
            );

            startLine.countDown();

            return List.of(
                pending.get(0).get(30, TimeUnit.SECONDS),
                pending.get(1).get(30, TimeUnit.SECONDS)
            );
        }
    }

    private Response awaitAndRefresh(CountDownLatch startLine, String refreshToken) throws InterruptedException {

        startLine.await();
        return refresh(refreshToken);
    }

    private List<String> userTokensOf(String accountId) {

        var tokens = redisTemplate.opsForSet().members(RedisKeySpace.forUserTokens(accountId));

        return tokens == null
            ? List.of()
            : List.copyOf(tokens);
    }

    private static String[] blankRefreshTokens() {

        return new String[]{null, "", " "};
    }
}
