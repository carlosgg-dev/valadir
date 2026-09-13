package com.valadir.e2e;

import com.valadir.common.error.ErrorCode;
import com.valadir.security.redis.RedisKeySpace;
import com.valadir.web.config.ApiRoutes;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class AccountDeletionIT extends AbstractAuthE2EIT {

    private static final String EMAIL = "bruce.wayne@email.com";
    private static final String BYSTANDER_EMAIL = "clark.kent@email.com";
    private static final String PASSWORD = "SecureP@ss123";
    private static final String WRONG_PASSWORD = "Wrong@password123";
    private static final String CAPTCHA_TOKEN = "e2e-captcha-token";

    // Mirrors auth.lockout: the challenge threshold and the first tier.
    private static final int CHALLENGE_THRESHOLD = 3;
    private static final int FIRST_TIER_FAILURES = 5;
    private static final Duration FIRST_TIER_LOCKOUT = Duration.ofSeconds(60);

    @Test
    void deleteAccount_correctPassword_returns204AndRemovesAccountAndProfile() {

        registerAndActivate(EMAIL, PASSWORD);

        var accountId = UUID.fromString(accountIdFor(EMAIL));

        deleteAccount(accessTokenOf(login(EMAIL, PASSWORD)), PASSWORD)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(accountJpaRepository.findById(accountId)).isEmpty();
        assertThat(userJpaRepository.findByAccountId(accountId)).isEmpty();
    }

    @Test
    void deleteAccount_liveSessionsOnTwoDevices_stopAuthenticating() {

        registerAndActivate(EMAIL, PASSWORD);

        String accountId = accountIdFor(EMAIL);
        String callingDeviceAccessToken = accessTokenOf(login(EMAIL, PASSWORD));

        Response otherDeviceLogin = login(EMAIL, PASSWORD);
        String otherDeviceAccessToken = accessTokenOf(otherDeviceLogin);
        String otherDeviceRefreshToken = refreshTokenOf(otherDeviceLogin);

        deleteAccount(callingDeviceAccessToken, PASSWORD)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        // Nothing checks that the account still exists on an authenticated request: only the
        // revocation stands between this token and the next 15 minutes.
        logoutAll(otherDeviceAccessToken)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.AUTHENTICATION_REQUIRED.getCode()));

        // A refresh token left in the whitelist would reach an account that no longer exists and
        // answer 500: the 401 is what proves the session was revoked, not merely orphaned.
        refresh(otherDeviceRefreshToken)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.INVALID_TOKEN.getCode()));

        assertThat(sessionFingerprintsFor(accountId)).isEmpty();
    }

    @Test
    void deleteAccount_twoAccounts_removesOnlyTheCallersAccount() {

        registerAndActivate(EMAIL, PASSWORD);
        registerAndActivate(BYSTANDER_EMAIL, PASSWORD);

        String callingAccessToken = accessTokenOf(login(EMAIL, PASSWORD));

        // The bystander never calls: its account and its session are what the deletion must not reach.
        Response bystanderLogin = login(BYSTANDER_EMAIL, PASSWORD);
        String bystanderAccessToken = accessTokenOf(bystanderLogin);
        String bystanderRefreshToken = refreshTokenOf(bystanderLogin);
        String bystanderAccountId = accountIdFor(BYSTANDER_EMAIL);

        deleteAccount(callingAccessToken, PASSWORD)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(accountJpaRepository.findById(UUID.fromString(bystanderAccountId))).isPresent();
        assertThat(userJpaRepository.findByAccountId(UUID.fromString(bystanderAccountId))).isPresent();

        assertThat(sessionFingerprintsFor(bystanderAccountId)).containsExactly(fingerprintOf(bystanderRefreshToken));
        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forTokenCutoff(bystanderAccountId))).isNull();

        logoutAll(bystanderAccessToken)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void deleteAccount_wrongPassword_returns401AndLeavesAccountAndSessionsIntact() {

        registerAndActivate(EMAIL, PASSWORD);

        String accountId = accountIdFor(EMAIL);
        Response loggedIn = login(EMAIL, PASSWORD);
        String refreshToken = refreshTokenOf(loggedIn);

        deleteAccount(accessTokenOf(loggedIn), WRONG_PASSWORD)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode()));

        assertThat(accountJpaRepository.findById(UUID.fromString(accountId))).isPresent();

        // A password checked after the revocation would still answer 401 here, with every session gone.
        assertThat(sessionFingerprintsFor(accountId)).containsExactly(fingerprintOf(refreshToken));
        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forTokenCutoff(accountId))).isNull();
    }

    @Test
    void deleteAccount_repeatedWrongPasswords_locksLoginOfThatEmail() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String accessToken = accessTokenOf(loggedIn);

        // Past the challenge threshold too: no CAPTCHA step-up stops an authenticated caller.
        failDeletionTimes(accessToken, FIRST_TIER_FAILURES);

        // A login that never failed is denied only if it reads the failures the deletion recorded:
        // with a counter of its own, this would answer 200.
        login(EMAIL, PASSWORD, CAPTCHA_TOKEN)
            .then()
            .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
            .body("code", equalTo(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED.getCode()));

        // The right password does not get past the lockout either.
        deleteAccount(accessToken, PASSWORD)
            .then()
            .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
            .body("code", equalTo(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED.getCode()));

        assertThat(accountJpaRepository.findByEmail(EMAIL)).isPresent();
        assertThat(accountLockedNotifier.lastLockoutFor(EMAIL)).contains(FIRST_TIER_LOCKOUT);
    }

    @Test
    void deleteAccount_afterFailedAttempts_letsTheEmailRegisterAndSignInAfresh() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String accessToken = accessTokenOf(loggedIn);

        // Enough failures to trip the challenge, short of the lockout that would deny the deletion itself.
        failDeletionTimes(accessToken, CHALLENGE_THRESHOLD);

        deleteAccount(accessToken, PASSWORD)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        registerAndActivate(EMAIL, PASSWORD);

        // The counter is keyed by email and outlives the account unless the deletion clears it:
        // the new account would open on a CAPTCHA challenge it never earned.
        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    void deleteAccount_withoutBearerToken_returns401AndKeepsTheAccount() {

        registerAndActivate(EMAIL, PASSWORD);

        deleteAccount(PASSWORD)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.AUTHENTICATION_REQUIRED.getCode()));

        assertThat(accountJpaRepository.findByEmail(EMAIL)).isPresent();
    }

    // Anonymous call against a protected route: no Authorization header.
    private Response deleteAccount(String password) {

        return deleteAccount(null, password);
    }

    private Response deleteAccount(String accessToken, String password) {

        var request = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of("password", password));

        if (accessToken != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }

        return request
            .when()
            .post(ApiRoutes.Auth.Account.DELETE_PATH);
    }

    private void failDeletionTimes(String accessToken, int times) {

        IntStream.range(0, times).forEach(i -> deleteAccount(accessToken, WRONG_PASSWORD)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode())));
    }
}
