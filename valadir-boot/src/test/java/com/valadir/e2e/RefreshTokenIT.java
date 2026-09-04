package com.valadir.e2e;

import com.valadir.common.error.ErrorCode;
import com.valadir.test.mother.PasswordMother;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

class RefreshTokenIT extends AbstractAuthE2EIT {

    private static final String EMAIL = "bruce.wayne@email.com";
    private static final String BYSTANDER_EMAIL = "clark.kent@email.com";
    private static final String PASSWORD = PasswordMother.raw().value();

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

        String oldTokenKey = refreshTokenKeyOf(oldRefreshToken);
        assertThat(redisTemplate.opsForValue().get(oldTokenKey)).isNull();

        String accountId = accountIdFor(EMAIL);
        String newTokenKey = refreshTokenKeyOf(newRefreshToken);

        assertThat(redisTemplate.opsForValue().get(newTokenKey)).isEqualTo(accountId);
        assertThat(redisTemplate.getExpire(newTokenKey))
            .isBetween(REFRESH_TOKEN_TTL.minusMinutes(1).toSeconds(), REFRESH_TOKEN_TTL.toSeconds());

        // Still exactly one session: the rotation swapped the member instead of piling up a second
        // one, so the set does not leak an entry per refresh over the token's 7-day life.
        assertThat(sessionFingerprintsFor(accountId)).containsExactly(fingerprintOf(newRefreshToken));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void refresh_blankToken_returns400AndLeavesSessionIntact(String blankRefreshToken) {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String refreshToken = refreshTokenOf(loggedIn);

        refresh(blankRefreshToken)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo(ErrorCode.INVALID_FIELD.getCode()))
            .body("errors.field", hasItem("refreshToken"));

        String accountId = accountIdFor(EMAIL);

        // Bean Validation rejects the request before the use case runs, so nothing was rotated.
        assertThat(sessionFingerprintsFor(accountId)).containsExactly(fingerprintOf(refreshToken));
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
            .body("code", equalTo(ErrorCode.INVALID_TOKEN.getCode()))
            .body("errors", nullValue());

        String accountId = accountIdFor(EMAIL);

        // The replay must not take the live session down with it: rejecting a spent token is not a
        // reason to revoke the one the legitimate user is holding.
        assertThat(sessionFingerprintsFor(accountId)).containsExactly(fingerprintOf(newRefreshToken));
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

        String accountId = accountIdFor(EMAIL);

        assertThat(sessionFingerprintsFor(accountId))
            .containsExactlyInAnyOrder(fingerprintOf(rotatedToken), fingerprintOf(bystanderDeviceToken));

        assertThat(redisTemplate.opsForValue().get(refreshTokenKeyOf(bystanderDeviceToken)))
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

        String rotatingAccountId = accountIdFor(EMAIL);
        String bystanderAccountId = accountIdFor(BYSTANDER_EMAIL);

        assertThat(redisTemplate.opsForValue().get(refreshTokenKeyOf(rotatedToken)))
            .isEqualTo(rotatingAccountId);
        assertThat(sessionFingerprintsFor(rotatingAccountId)).containsExactly(fingerprintOf(rotatedToken));

        assertThat(redisTemplate.opsForValue().get(refreshTokenKeyOf(bystanderAccountToken)))
            .isEqualTo(bystanderAccountId);
        assertThat(sessionFingerprintsFor(bystanderAccountId)).containsExactly(fingerprintOf(bystanderAccountToken));
    }

    @Test
    void refresh_concurrentUseOfSameToken_grantsExactlyOneRotation() {

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
            .body("code", equalTo(ErrorCode.INVALID_TOKEN.getCode()));

        String winningToken = refreshTokenOf(accepted.getFirst());
        String accountId = accountIdFor(EMAIL);

        assertThat(sessionFingerprintsFor(accountId)).containsExactly(fingerprintOf(winningToken));
    }

    @Test
    void refresh_twoSessionsRotatingConcurrently_keepsBothInTheSet() {

        registerAndActivate(EMAIL, PASSWORD);

        Response firstDeviceLogin = login(EMAIL, PASSWORD);
        Response secondDeviceLogin = login(EMAIL, PASSWORD);
        String firstDeviceToken = refreshTokenOf(firstDeviceLogin);
        String secondDeviceToken = refreshTokenOf(secondDeviceLogin);

        // Two devices renewing at once write to one set: each rotation has to SREM its own member
        // and SADD the replacement without either losing the other. Split out of the Lua script,
        // the interleaving would drop a live session and nothing else in the suite would notice.
        List<Response> responses = concurrently(List.of(
            () -> refresh(firstDeviceToken),
            () -> refresh(secondDeviceToken)
        ));

        responses.forEach(response -> response.then().statusCode(HttpStatus.OK.value()));

        String accountId = accountIdFor(EMAIL);

        assertThat(sessionFingerprintsFor(accountId))
            .containsExactlyInAnyOrder(
                fingerprintOf(refreshTokenOf(responses.get(0))),
                fingerprintOf(refreshTokenOf(responses.get(1)))
            );

        assertThat(redisTemplate.hasKey(refreshTokenKeyOf(firstDeviceToken))).isFalse();
        assertThat(redisTemplate.hasKey(refreshTokenKeyOf(secondDeviceToken))).isFalse();
    }

    // Released together, so both requests reach Redis in the same window. Just submitting them would
    // let the first finish before the second starts — no race at all, only the spent-token path
    // another case already covers.
    private List<Response> refreshConcurrently(String refreshToken) {

        return concurrently(2, () -> refresh(refreshToken));
    }

}
