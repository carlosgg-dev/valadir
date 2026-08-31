package com.valadir.e2e;

import com.valadir.common.error.ErrorCode;
import com.valadir.security.redis.RedisKeySpace;
import com.valadir.test.mother.PasswordMother;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

class LogoutAllIT extends AbstractAuthE2EIT {

    private static final String EMAIL = "bruce.wayne@email.com";
    private static final String BYSTANDER_EMAIL = "clark.kent@email.com";
    private static final String PASSWORD = PasswordMother.raw().value();

    // Mirrors auth.jwt.access-token-ttl. application-test.yml does not redeclare it.
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    // What the revocation writes. Whether anything reads it is the next test's question.
    @Test
    void logoutAll_twoLiveSessions_returns204AndClearsEverySessionKey() {

        registerAndActivate(EMAIL, PASSWORD);

        Response callingDeviceLogin = login(EMAIL, PASSWORD);
        String callingDeviceAccessToken = accessTokenOf(callingDeviceLogin);
        String callingDeviceRefreshToken = refreshTokenOf(callingDeviceLogin);

        Response otherDeviceLogin = login(EMAIL, PASSWORD);
        String otherDeviceRefreshToken = refreshTokenOf(otherDeviceLogin);

        String accountId = accountIdFor(EMAIL);

        assertThat(sessionFingerprintsFor(accountId))
            .containsExactlyInAnyOrder(fingerprintOf(callingDeviceRefreshToken), fingerprintOf(otherDeviceRefreshToken));

        logoutAll(callingDeviceAccessToken)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(redisTemplate.opsForValue().get(refreshTokenKeyOf(callingDeviceRefreshToken))).isNull();
        assertThat(redisTemplate.opsForValue().get(refreshTokenKeyOf(otherDeviceRefreshToken))).isNull();
        assertThat(sessionFingerprintsFor(accountId)).isEmpty();

        String cutoffKey = RedisKeySpace.forTokenCutoff(accountId);

        assertThat(redisTemplate.opsForValue().get(cutoffKey)).isNotNull();

        // The cutoff only has to outlive the access tokens it refuses: past that span nothing it
        // could reject is still alive, and a key without a TTL would deny the account forever.
        assertThat(redisTemplate.getExpire(cutoffKey)).isBetween(1L, ACCESS_TOKEN_TTL.toSeconds());
    }

    // What those keys are worth: access tokens refused by time, the refresh token by absence.
    @Test
    void logoutAll_liveTokensOfEverySession_stopAuthenticating() {

        registerAndActivate(EMAIL, PASSWORD);

        Response callingDeviceLogin = login(EMAIL, PASSWORD);
        String callingDeviceAccessToken = accessTokenOf(callingDeviceLogin);

        Response otherDeviceLogin = login(EMAIL, PASSWORD);
        String otherDeviceAccessToken = accessTokenOf(otherDeviceLogin);
        String otherDeviceRefreshToken = refreshTokenOf(otherDeviceLogin);

        logoutAll(callingDeviceAccessToken)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        // Closing every session includes the one making the call.
        logoutAll(callingDeviceAccessToken)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.AUTHENTICATION_REQUIRED.getCode()))
            .body("errors", nullValue());

        // No jti of this one ever reached the blacklist: only the cutoff can refuse it.
        logoutAll(otherDeviceAccessToken)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());

        // Another store: gone from the whitelist, so the session cannot be minted again.
        refresh(otherDeviceRefreshToken)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void logoutAll_twoAccountsWithLiveSessions_revokesOnlyTheCallersAccount() {

        registerAndActivate(EMAIL, PASSWORD);
        registerAndActivate(BYSTANDER_EMAIL, PASSWORD);

        String callingAccountAccessToken = accessTokenOf(login(EMAIL, PASSWORD));

        // The bystander account never calls: its session is the state the revocation must not reach.
        Response bystanderLogin = login(BYSTANDER_EMAIL, PASSWORD);
        String bystanderAccessToken = accessTokenOf(bystanderLogin);
        String bystanderRefreshToken = refreshTokenOf(bystanderLogin);

        logoutAll(callingAccountAccessToken)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String callingAccountId = accountIdFor(EMAIL);
        String bystanderAccountId = accountIdFor(BYSTANDER_EMAIL);

        assertThat(sessionFingerprintsFor(callingAccountId)).isEmpty();

        assertThat(redisTemplate.opsForValue().get(refreshTokenKeyOf(bystanderRefreshToken)))
            .isEqualTo(bystanderAccountId);
        assertThat(sessionFingerprintsFor(bystanderAccountId)).containsExactly(fingerprintOf(bystanderRefreshToken));

        // The cutoff is written per account: an account-wide key would sign the bystander out too,
        // and no refresh-token assertion above would notice.
        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forTokenCutoff(bystanderAccountId))).isNull();

        logout(bystanderAccessToken, bystanderRefreshToken)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void logoutAll_withoutBearerToken_returns401AndLeavesSessionsIntact() {

        registerAndActivate(EMAIL, PASSWORD);

        String refreshToken = refreshTokenOf(login(EMAIL, PASSWORD));

        logoutAll()
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.AUTHENTICATION_REQUIRED.getCode()))
            .body("errors", nullValue());

        String accountId = accountIdFor(EMAIL);

        assertThat(redisTemplate.opsForValue().get(refreshTokenKeyOf(refreshToken))).isNotNull();
        assertThat(sessionFingerprintsFor(accountId)).containsExactly(fingerprintOf(refreshToken));
        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forTokenCutoff(accountId))).isNull();
    }
}
