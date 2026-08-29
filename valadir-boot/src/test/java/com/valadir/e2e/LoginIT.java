package com.valadir.e2e;

import com.valadir.common.error.ErrorCode;
import com.valadir.security.redis.RedisKeySpace;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

class LoginIT extends AbstractAuthE2EIT {

    private static final String EMAIL = "bruce.wayne@email.com";
    private static final String UNKNOWN_EMAIL = "unknown@email.test";
    private static final String PASSWORD = "SecureP@ss123";
    private static final String WRONG_PASSWORD = "Wrong@password123";
    private static final String CAPTCHA_TOKEN = "e2e-captcha-token";

    // The flip points these cases drive. What auth.lockout binds to is pinned by
    // ProductionConfigurationTest; what those numbers do to a login is pinned here.
    private static final int CHALLENGE_THRESHOLD = 3;
    private static final int FIRST_TIER_FAILURES = 5;
    private static final int SECOND_TIER_FAILURES = 10;
    private static final Duration FIRST_TIER_LOCKOUT = Duration.ofSeconds(60);
    private static final Duration SECOND_TIER_LOCKOUT = Duration.ofMinutes(5);
    private static final Duration ATTEMPT_WINDOW = Duration.ofHours(1);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    @Test
    void login_validCredentials_returnsTokensAndPersistsRefreshTokenInRedis() {

        registerAndActivate(EMAIL, PASSWORD);

        String refreshToken = login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("accessToken", notNullValue())
            .body("refreshToken", notNullValue())
            .extract().path("refreshToken");

        String accountId = accountIdOf(EMAIL);
        String tokenKey = refreshTokenKeyOf(refreshToken);

        assertThat(redisTemplate.opsForValue().get(tokenKey)).isEqualTo(accountId);
        assertThat(redisTemplate.getExpire(tokenKey))
            .isBetween(REFRESH_TOKEN_TTL.minusMinutes(1).toSeconds(), REFRESH_TOKEN_TTL.toSeconds());

        // Exactly one session: a fresh login must not leave duplicate or stray tokens behind.
        assertThat(redisTemplate.opsForSet().size(RedisKeySpace.forUserTokens(accountId))).isEqualTo(1);
        assertThat(redisTemplate.opsForSet().isMember(RedisKeySpace.forUserTokens(accountId), fingerprintOf(refreshToken)))
            .isTrue();

        assertThat(failedAttemptsFor(EMAIL)).isNull();
    }

    @Test
    void login_repeatedFailuresOnRegisteredAccount_challengesThenLocks() {

        registerAndActivate(EMAIL, PASSWORD);

        assertChallengeThenLockoutProgression(EMAIL);
    }

    @Test
    void login_repeatedFailuresOnUnknownEmail_challengesThenLocksIdentically() {

        assertChallengeThenLockoutProgression(UNKNOWN_EMAIL);
    }

    @Test
    void login_unknownEmailLockedOut_notifiesNobody() {

        driveToFirstTierLockout(UNKNOWN_EMAIL);

        login(UNKNOWN_EMAIL, WRONG_PASSWORD, CAPTCHA_TOKEN)
            .then()
            .statusCode(HttpStatus.TOO_MANY_REQUESTS.value());

        // No owner to warn: locking out a phantom address must never send mail.
        assertThat(accountLockedNotifier.capturedNothing()).isTrue();
    }

    @Test
    void login_secondLogin_addsSessionWithoutRevokingTheFirst() {

        registerAndActivate(EMAIL, PASSWORD);

        Response firstLogin = login(EMAIL, PASSWORD);
        String firstRefreshToken = refreshTokenOf(firstLogin);

        Response secondLogin = login(EMAIL, PASSWORD);
        String secondRefreshToken = refreshTokenOf(secondLogin);

        String accountId = accountIdOf(EMAIL);

        // Login adds a session, it does not replace one. This flow is what mutates the set, so it is
        // where the semantics belong — RefreshTokenIT logs in twice, but to rotate, not to assert this.
        assertThat(sessionFingerprintsOf(accountId))
            .containsExactlyInAnyOrder(fingerprintOf(firstRefreshToken), fingerprintOf(secondRefreshToken));
    }

    @Test
    void login_wrongPassword_returnsOpaque401AndAccumulatesFailedAttempt() {

        registerAndActivate(EMAIL, PASSWORD);

        login(EMAIL, WRONG_PASSWORD)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode()))
            .body("errors", nullValue());

        assertThat(failedAttemptsFor(EMAIL)).isEqualTo("1");

        // The counter must live exactly as long as the fixed attempt window (auth.lockout.window):
        // without the TTL, failures would accumulate forever and lock accounts unfairly.
        assertThat(redisTemplate.getExpire(RedisKeySpace.forLoginAttempts(EMAIL)))
            .isBetween(1L, ATTEMPT_WINDOW.toSeconds());

        assertNoRefreshTokenIssued();
    }

    @Test
    void login_challengeActiveAndCaptchaTokenMissing_returns403AndDoesNotAccumulateFailure() {

        registerAndActivate(EMAIL, PASSWORD);
        failLoginTimes(EMAIL, CHALLENGE_THRESHOLD);

        // login without captcha (required)
        login(EMAIL, WRONG_PASSWORD)
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .body("code", equalTo(ErrorCode.CAPTCHA_REQUIRED.getCode()))
            .body("errors", nullValue());

        // The rejected step-up never reaches the password check: the counter must remain at its current
        // value so that a subsequent attempt using a token will still count as the next failure
        assertThat(failedAttemptsFor(EMAIL)).isEqualTo(String.valueOf(CHALLENGE_THRESHOLD));
    }

    @Test
    void login_challengeSatisfiedWithValidCaptcha_proceedsToCredentialCheck() {

        registerAndActivate(EMAIL, PASSWORD);
        failLoginTimes(EMAIL, CHALLENGE_THRESHOLD);

        login(EMAIL, WRONG_PASSWORD, CAPTCHA_TOKEN)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode()));

        assertThat(failedAttemptsFor(EMAIL)).isEqualTo(String.valueOf(CHALLENGE_THRESHOLD + 1));

        login(EMAIL, PASSWORD, CAPTCHA_TOKEN)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("accessToken", notNullValue());

        // Success clears the failure history (attempts and lockout keys): the account starts
        // over instead of dragging 4 failures into the next mistyped password.
        assertThat(failedAttemptsFor(EMAIL)).isNull();
    }

    @Test
    void login_captchaProviderRejectsToken_returns403EvenWithTokenPresent() {

        registerAndActivate(EMAIL, PASSWORD);
        failLoginTimes(EMAIL, CHALLENGE_THRESHOLD);

        captchaVerifier.rejectAllTokens();

        login(EMAIL, PASSWORD, CAPTCHA_TOKEN)
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .body("code", equalTo(ErrorCode.CAPTCHA_REQUIRED.getCode()));

        // The error counter does not accumulate (it does not reach the password check)
        assertThat(failedAttemptsFor(EMAIL)).isEqualTo(String.valueOf(CHALLENGE_THRESHOLD));

        // The password was right: if the gate leaked past the rejected challenge, a session
        // would have been minted. No token in Redis proves the credential check never ran.
        assertNoRefreshTokenIssued();
    }

    @Test
    void login_fifthFailure_locksAccountAndNotifiesOwnerAsync() {

        registerAndActivate(EMAIL, PASSWORD);
        driveToFirstTierLockout(EMAIL);

        Response lockedOut = login(EMAIL, PASSWORD, CAPTCHA_TOKEN);

        lockedOut.then()
            .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
            .body("code", equalTo(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED.getCode()))
            .body("errors", nullValue());

        assertThat(Long.parseLong(lockedOut.header("Retry-After")))
            .isBetween(1L, FIRST_TIER_LOCKOUT.toSeconds());

        String lockoutKey = RedisKeySpace.forLoginLockout(EMAIL);
        assertThat(redisTemplate.opsForValue().get(lockoutKey))
            .isEqualTo(RedisKeySpace.LOGIN_LOCKOUT_VALUE);

        assertThat(redisTemplate.getExpire(lockoutKey))
            .isBetween(1L, FIRST_TIER_LOCKOUT.toSeconds());

        // The locked-out attempt dies at the gate, before recordFailedAttempt: retries during a
        // lockout must not escalate the counter toward the next tier.
        assertThat(failedAttemptsFor(EMAIL)).isEqualTo(String.valueOf(FIRST_TIER_FAILURES));

        // A locked account must not mint a session even when the credentials are right.
        assertNoRefreshTokenIssued();

        assertThat(accountLockedNotifier.lastLockoutFor(EMAIL)).contains(FIRST_TIER_LOCKOUT);
    }

    @Test
    void login_seededAttemptsCrossSecondTier_locksWithEscalatedRetryAfter() {

        registerAndActivate(EMAIL, PASSWORD);

        // Seeding trades HTTP purity for speed: reaching tier 2 over pure HTTP would mean
        // waiting out the 60s tier-1 lockout. The tier mapping itself is unit-covered
        // (LoginLockoutPolicyTest); this pins the end-to-end binding of the escalation config.
        redisTemplate.opsForValue()
            .set(RedisKeySpace.forLoginAttempts(EMAIL), String.valueOf(SECOND_TIER_FAILURES - 1), ATTEMPT_WINDOW);

        failLoginWithCaptcha(EMAIL);

        Response lockedOut = login(EMAIL, PASSWORD, CAPTCHA_TOKEN);

        lockedOut.then()
            .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
            .body("code", equalTo(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED.getCode()));

        assertThat(Long.parseLong(lockedOut.header("Retry-After")))
            .isBetween(SECOND_TIER_LOCKOUT.minusSeconds(30).toSeconds(), SECOND_TIER_LOCKOUT.toSeconds());

        assertThat(redisTemplate.getExpire(RedisKeySpace.forLoginLockout(EMAIL)))
            .isBetween(SECOND_TIER_LOCKOUT.minusSeconds(30).toSeconds(), SECOND_TIER_LOCKOUT.toSeconds());

        // Applying a tier must not reset the counter: the window is what carries failures across
        // expired lockouts, so a reset here would leave the 15-failure tier unreachable and cap
        // every attacker at 5 minutes forever.
        assertThat(failedAttemptsFor(EMAIL)).isEqualTo(String.valueOf(SECOND_TIER_FAILURES));

        // The owner notification must carry the escalated duration, not just the Retry-After
        // header: it is what the "your account is locked for N minutes" email tells the user.
        assertThat(accountLockedNotifier.lastLockoutFor(EMAIL)).contains(SECOND_TIER_LOCKOUT);
    }

    @Test
    void login_pendingActivationAccount_returns403AndIssuesNoSession() {

        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .body("code", equalTo(ErrorCode.ACCOUNT_PENDING_ACTIVATION.getCode()))
            .body("errors", nullValue());

        // The password is right: only the activation check stands between this request and a
        // session, so a regression here would hand tokens to an unverified email address.
        assertNoRefreshTokenIssued();

        // Neither a credential failure nor a successful login: the attempt state must stay
        // untouched, so mistyping the password later still starts counting from one.
        assertThat(failedAttemptsFor(EMAIL)).isNull();
    }

    private void assertChallengeThenLockoutProgression(String email) {

        failLoginTimes(email, CHALLENGE_THRESHOLD);

        login(email, WRONG_PASSWORD)
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .body("code", equalTo(ErrorCode.CAPTCHA_REQUIRED.getCode()))
            .body("errors", nullValue());

        failLoginWithCaptcha(email);
        failLoginWithCaptcha(email);

        login(email, WRONG_PASSWORD, CAPTCHA_TOKEN)
            .then()
            .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
            .body("code", equalTo(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED.getCode()))
            .body("errors", nullValue());
    }

    private void driveToFirstTierLockout(String email) {

        failLoginTimes(email, CHALLENGE_THRESHOLD);
        failLoginWithCaptcha(email);
        failLoginWithCaptcha(email);
    }

    private void failLoginTimes(String email, int times) {

        IntStream.range(0, times).forEach(i -> login(email, WRONG_PASSWORD)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode())));
    }

    private void failLoginWithCaptcha(String email) {

        login(email, WRONG_PASSWORD, CAPTCHA_TOKEN)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode()));
    }

    private String failedAttemptsFor(String email) {

        return redisTemplate.opsForValue().get(RedisKeySpace.forLoginAttempts(email));
    }

    // KEYS is O(n) and forbidden in production code, but the test Redis holds a handful of
    // entries and the pattern scan guarantees no token escaped under any key.
    private void assertNoRefreshTokenIssued() {

        assertThat(redisTemplate.keys(RedisKeySpace.REFRESH_TOKEN_PREFIX + "*")).isEmpty();
    }
}
