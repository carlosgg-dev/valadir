package com.valadir.e2e;

import com.valadir.security.redis.RedisKeySpace;
import com.valadir.security.redis.TokenFingerprint;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

class PasswordResetIT extends AbstractAuthE2EIT {

    private static final String EMAIL = "bruce.wayne@email.com";
    private static final String BYSTANDER_EMAIL = "clark.kent@email.com";
    private static final String UNKNOWN_EMAIL = "unknown@email.test";

    private static final String PASSWORD = "SecureP@ss123";
    private static final String NEW_PASSWORD = "AnotherP@ss456";

    // Rejected before hashing: fails the RawPassword policy
    private static final String TOO_SHORT_PASSWORD = "Short1@";
    // Rejected before hashing: contains the full name
    private static final String PERSONAL_DATA_PASSWORD = "BruceWayne@1";

    private static final String INSECURE_PASSWORD_CODE = "BIZ-001";
    private static final String INVALID_RESET_OTP_CODE = "BIZ-006";
    private static final String INVALID_VERIFICATION_TOKEN_CODE = "BIZ-007";
    private static final String INVALID_CREDENTIALS_CODE = "SEC-001";
    private static final String AUTHENTICATION_REQUIRED_CODE = "SEC-003";
    private static final String INVALID_PASSWORD_CODE = "VAL-002";

    // Mirror auth.password-reset.*. application-test.yml does not redeclare them, so unlike the
    // JWT TTLs these pin the production binding from application.yml.
    private static final Duration RESET_OTP_TTL = Duration.ofMinutes(15);
    private static final Duration VERIFICATION_TOKEN_TTL = Duration.ofMinutes(10);

    @Test
    void initiatePasswordReset_activeAccount_returns204AndStoresOtp() {

        registerAndActivate(EMAIL, PASSWORD);

        initiatePasswordReset(EMAIL)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String accountId = accountIdOf(EMAIL);
        String otpKey = RedisKeySpace.forPasswordResetOtp(accountId);
        String deliveredOtp = passwordResetOtpFor(EMAIL);

        // What Redis holds is a hash, not the plaintext code
        assertThat(redisTemplate.opsForValue().get(otpKey))
            .isNotNull()
            .doesNotContain(deliveredOtp);

        assertThat(redisTemplate.getExpire(otpKey))
            .isBetween(RESET_OTP_TTL.minusMinutes(1).toSeconds(), RESET_OTP_TTL.toSeconds());
    }

    @Test
    void initiatePasswordReset_unknownEmail_returns204AndSendsNothing() {

        // Same 204 a real account gets: the response must not tell whether the email is registered,
        // and no mail may be sent to an address nobody signed up with.
        initiatePasswordReset(UNKNOWN_EMAIL)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(passwordResetNotifier.lastOtpFor(UNKNOWN_EMAIL)).isEmpty();
        assertThat(redisTemplate.keys(RedisKeySpace.forPasswordResetOtp("*"))).isEmpty();
    }

    @Test
    void initiatePasswordReset_pendingActivationAccount_returns204AndSendsNothing() {

        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        initiatePasswordReset(EMAIL)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        // An unverified account has no reset path: completing one only changes the password, and login
        // still rejects PENDING_ACTIVATION. Activation is the single way out of that state.
        assertThat(passwordResetNotifier.lastOtpFor(EMAIL)).isEmpty();
        assertThat(redisTemplate.keys(RedisKeySpace.forPasswordResetOtp("*"))).isEmpty();
    }

    @Test
    void verifyPasswordResetOtp_capturedOtp_returns200WithVerificationTokenAndSpendsTheOtp() {

        registerAndActivate(EMAIL, PASSWORD);

        initiatePasswordReset(EMAIL)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String accountId = accountIdOf(EMAIL);
        String resetOtp = passwordResetOtpFor(EMAIL);

        Response verified = verifyPasswordResetOtp(EMAIL, resetOtp)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("verificationToken", notNullValue())
            .extract()
            .response();

        String verificationToken = verificationTokenOf(verified);
        String tokenKey = RedisKeySpace.forPasswordResetVerificationToken(TokenFingerprint.of(verificationToken));

        // The token is what carries the account forward into complete: filed under the wrong owner,
        // it would reset somebody else's password.
        assertThat(redisTemplate.opsForValue().get(tokenKey)).isEqualTo(accountId);
        assertThat(redisTemplate.getExpire(tokenKey))
            .isBetween(VERIFICATION_TOKEN_TTL.minusMinutes(1).toSeconds(), VERIFICATION_TOKEN_TTL.toSeconds());

        assertThat(redisTemplate.hasKey(RedisKeySpace.forPasswordResetOtp(accountId))).isFalse();

        // Single use: spending the code must not leave it usable for a second verification, which
        // would hand out a fresh token to anyone who read the same email later.
        verifyPasswordResetOtp(EMAIL, resetOtp)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_RESET_OTP_CODE));
    }

    @Test
    void verifyPasswordResetOtp_unknownEmail_returns401WithoutRevealingTheAccountIsMissing() {

        // Indistinguishable from a wrong code against a real account, so the response is not an
        // existence oracle.
        verifyPasswordResetOtp(UNKNOWN_EMAIL, "123456")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_RESET_OTP_CODE))
            .body("errors", nullValue());
    }

    @Test
    void verifyPasswordResetOtp_expiredOtp_returns401() {

        registerAndActivate(EMAIL, PASSWORD);

        initiatePasswordReset(EMAIL)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String accountId = accountIdOf(EMAIL);
        String resetOtp = passwordResetOtpFor(EMAIL);

        // What the TTL leaves behind: the code gone from Redis while the owner still holds it.
        redisTemplate.delete(RedisKeySpace.forPasswordResetOtp(accountId));

        verifyPasswordResetOtp(EMAIL, resetOtp)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_RESET_OTP_CODE))
            .body("errors", nullValue());
    }

    @Test
    void verifyPasswordResetOtp_wrongOtp_returns401AndKeepsTheOtpUsable() {

        registerAndActivate(EMAIL, PASSWORD);

        initiatePasswordReset(EMAIL)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String resetOtp = passwordResetOtpFor(EMAIL);

        verifyPasswordResetOtp(EMAIL, otherOtpThan(resetOtp))
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_RESET_OTP_CODE))
            .body("errors", nullValue());

        // A failed guess must not burn the real code: otherwise anyone knowing the email could
        // lock the owner out of their own reset by guessing once.
        verifyPasswordResetOtp(EMAIL, resetOtp)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    void verifyPasswordResetOtp_otpOfAnotherAccount_returns401() {

        registerAndActivate(EMAIL, PASSWORD);
        registerAndActivate(BYSTANDER_EMAIL, PASSWORD);

        initiatePasswordReset(EMAIL)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        initiatePasswordReset(BYSTANDER_EMAIL)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String bystanderOtp = passwordResetOtpFor(BYSTANDER_EMAIL);

        // The OTP is looked up under the account resolved from the email presented: a code that is
        // valid somewhere else must not verify this one.
        verifyPasswordResetOtp(EMAIL, bystanderOtp)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_RESET_OTP_CODE));

        // And it is still the bystander's own code, unspent by the failed crossing.
        verifyPasswordResetOtp(BYSTANDER_EMAIL, bystanderOtp)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    void completePasswordReset_verifiedToken_returns204AndChangesThePassword() {

        registerAndActivate(EMAIL, PASSWORD);

        initiatePasswordReset(EMAIL)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String resetOtp = passwordResetOtpFor(EMAIL);

        Response verified = verifyPasswordResetOtp(EMAIL, resetOtp)
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .response();

        String verificationToken = verificationTokenOf(verified);

        completePasswordReset(verificationToken, NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_CREDENTIALS_CODE));

        // Asserted through behaviour rather than by reading the hash: what matters is which
        // password opens the account.
        login(EMAIL, NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());

        String tokenKey = RedisKeySpace.forPasswordResetVerificationToken(TokenFingerprint.of(verificationToken));
        assertThat(redisTemplate.hasKey(tokenKey)).isFalse();
    }

    @Test
    void completePasswordReset_unknownToken_returns401() {

        registerAndActivate(EMAIL, PASSWORD);

        // The previous steps are not necessary
        completePasswordReset(UUID.randomUUID().toString(), NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_VERIFICATION_TOKEN_CODE))
            .body("errors", nullValue());

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    void completePasswordReset_reusedToken_returns401() {

        registerAndActivate(EMAIL, PASSWORD);

        initiatePasswordReset(EMAIL)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String resetOtp = passwordResetOtpFor(EMAIL);

        Response verified = verifyPasswordResetOtp(EMAIL, resetOtp)
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .response();

        String verificationToken = verificationTokenOf(verified);

        completePasswordReset(verificationToken, NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        // Single use: a token read from a leaked email must not let a second password be set later.
        completePasswordReset(verificationToken, "YetAnotherP@ss789")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_VERIFICATION_TOKEN_CODE))
            .body("errors", nullValue());

        login(EMAIL, NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @ParameterizedTest
    @MethodSource("rejectedPasswords")
    void completePasswordReset_rejectedPassword_returns400AndKeepsTheOldPassword(String password, String expectedCode) {

        registerAndActivate(EMAIL, PASSWORD);

        initiatePasswordReset(EMAIL)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String resetOtp = passwordResetOtpFor(EMAIL);

        Response verified = verifyPasswordResetOtp(EMAIL, resetOtp)
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .response();

        String verificationToken = verificationTokenOf(verified);

        completePasswordReset(verificationToken, password)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo(expectedCode))
            .body("errors", nullValue());

        // Both guards live in the domain: if either stopped being wired into this use case, a weak
        // password would be accepted here and no unit test would notice.
        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());

        // The token survives a rejected password: being told the password is too weak must not
        // force the user back to their inbox for a new code.
        completePasswordReset(verificationToken, NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void completePasswordReset_activeSessions_revokesAllOfThem() {

        registerAndActivate(EMAIL, PASSWORD);

        Response firstDevice = login(EMAIL, PASSWORD);
        String firstDeviceToken = refreshTokenOf(firstDevice);

        Response secondDevice = login(EMAIL, PASSWORD);
        String secondDeviceToken = refreshTokenOf(secondDevice);

        resetPasswordTo(EMAIL, NEW_PASSWORD);

        // A reset is what a user does when they suspect the account is compromised: leaving any
        // device signed in would defeat the point of changing the password.
        refresh(firstDeviceToken)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());

        refresh(secondDeviceToken)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());

        String accountId = accountIdOf(EMAIL);
        assertThat(sessionFingerprintsOf(accountId)).isEmpty();
    }

    @Test
    void completePasswordReset_twoAccountsWithLiveSessions_revokesOnlyTheOwningAccount() {

        registerAndActivate(EMAIL, PASSWORD);
        registerAndActivate(BYSTANDER_EMAIL, PASSWORD);

        Response resettingAccountLogin = login(EMAIL, PASSWORD);
        String resettingAccountToken = refreshTokenOf(resettingAccountLogin);

        // The bystander account never resets: its session is the state the revocation must not reach.
        Response bystanderAccountLogin = login(BYSTANDER_EMAIL, PASSWORD);
        String bystanderAccountToken = refreshTokenOf(bystanderAccountLogin);
        String bystanderAccessToken = accessTokenOf(bystanderAccountLogin);

        resetPasswordTo(EMAIL, NEW_PASSWORD);

        String resettingAccountId = accountIdOf(EMAIL);
        String bystanderAccountId = accountIdOf(BYSTANDER_EMAIL);

        assertThat(redisTemplate.hasKey(refreshTokenKeyOf(resettingAccountToken))).isFalse();
        assertThat(sessionFingerprintsOf(resettingAccountId)).isEmpty();

        // Account-wide invalidation is the most damaging place to resolve the wrong account: with a
        // single account in the database, a mistake there would pass unnoticed.
        assertThat(redisTemplate.opsForValue().get(refreshTokenKeyOf(bystanderAccountToken)))
            .isEqualTo(bystanderAccountId);
        assertThat(sessionFingerprintsOf(bystanderAccountId)).containsExactly(fingerprintOf(bystanderAccountToken));

        // The cutoff is written per account: an account-wide key would sign the bystander out too,
        // and no refresh-token assertion above would notice.
        logout(bystanderAccessToken, bystanderAccountToken)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        // And the bystander's password is untouched: only the sessions were at stake above.
        login(BYSTANDER_EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    void completePasswordReset_liveAccessToken_stopsAuthenticating() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String accessToken = accessTokenOf(loggedIn);
        String refreshToken = refreshTokenOf(loggedIn);

        resetPasswordTo(EMAIL, NEW_PASSWORD);

        // Revoking the refresh token alone leaves a stolen access token authenticating for the rest
        // of its lifetime — the very window the user is trying to close by resetting the password.
        logout(accessToken, refreshToken)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(AUTHENTICATION_REQUIRED_CODE));
    }

    @Test
    void completePasswordReset_signingInAfterwards_authenticatesAgain() {

        registerAndActivate(EMAIL, PASSWORD);

        resetPasswordTo(EMAIL, NEW_PASSWORD);
        awaitTheNextSecond();

        Response loggedIn = login(EMAIL, NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .response();

        // The cutoff closes a window, it does not close the account: a token issued after it must
        // authenticate. A cutoff written into the future, or without a TTL, would deny this forever.
        logout(accessTokenOf(loggedIn), refreshTokenOf(loggedIn))
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    // Precondition: the whole reset flow, asserted step by step, so a test about what the reset
    // leaves behind fails on its outcome and never on a broken setup.
    private void resetPasswordTo(String email, String newPassword) {

        initiatePasswordReset(email)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        Response verified = verifyPasswordResetOtp(email, passwordResetOtpFor(email))
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .response();

        completePasswordReset(verificationTokenOf(verified), newPassword)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    // A JWT carries iat in whole seconds, so a token minted within the same second as the cutoff
    // cannot be proven newer than it and is denied. Crossing the tick is what makes the test above
    // measure the cutoff instead of that ambiguity.
    private static void awaitTheNextSecond() {

        long secondOfTheReset = Instant.now().getEpochSecond();

        Awaitility.await("the clock to cross into the second after the reset")
            .atMost(Duration.ofSeconds(2))
            .pollInterval(Duration.ofMillis(20))
            .until(() -> Instant.now().getEpochSecond() > secondOfTheReset);
    }

    private static Stream<Arguments> rejectedPasswords() {

        return Stream.of(
            Arguments.of(TOO_SHORT_PASSWORD, INVALID_PASSWORD_CODE),
            Arguments.of(PERSONAL_DATA_PASSWORD, INSECURE_PASSWORD_CODE)
        );
    }
}
