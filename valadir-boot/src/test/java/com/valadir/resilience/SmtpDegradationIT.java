package com.valadir.resilience;

import com.valadir.domain.model.AccountStatus;
import com.valadir.e2e.AbstractAuthE2EIT;
import com.valadir.security.redis.RedisKeySpace;
import com.valadir.test.mother.PasswordMother;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Mail is a secondary service, and the two halves of that sentence are different guarantees: an OTP
 * the user never received must leave a recoverable account, while a notification nobody is waiting
 * for must not change the outcome of the flow that triggered it.
 *
 * <p>Extends the flow base and not {@link AbstractResilienceIT} on purpose: nothing here is paused.
 * The outage is injected at the port with {@code failNextSend()}, so this class reuses the cached
 * context of the five flow ITs at no extra cost — adding a {@code @TestPropertySource} or an
 * {@code @Import} of its own would buy it a second one for nothing.
 */
class SmtpDegradationIT extends AbstractAuthE2EIT {

    private static final String EMAIL = "bruce.wayne@email.com";
    private static final String PASSWORD = PasswordMother.raw().value();
    private static final String WRONG_PASSWORD = "Wrong@password123";
    private static final String CAPTCHA_TOKEN = "e2e-captcha-token";
    private static final String INVALID_CREDENTIALS_CODE = "SEC-001";

    private static final int FIRST_TIER_FAILURES = 5;
    private static final Duration FIRST_TIER_LOCKOUT = Duration.ofSeconds(60);
    private static final Duration ATTEMPT_WINDOW = Duration.ofHours(1);

    @Test
    void register_smtpDown_persistsThePendingAccountAndFailsRetryable() {

        accountActivationNotifier.failNextSend();

        Response response = register(EMAIL, PASSWORD);

        // The account is committed before the OTP is ever sent, so the failure cannot roll it back.
        // Answering 503 rather than 500 is what tells the caller the attempt is worth repeating.
        assertOpaqueInfrastructureFailure(response);

        assertThat(accountStatusOf(EMAIL)).isEqualTo(AccountStatus.PENDING_ACTIVATION);

        // Nothing was delivered, so the code stored in Redis is one nobody can present.
        assertThat(accountActivationNotifier.lastOtpFor(EMAIL)).isEmpty();
    }

    @Test
    void login_lockoutNotificationFails_stillDeniesWithInvalidCredentials() {

        registerAndActivate(EMAIL, PASSWORD);

        // Seeded to one failure below the tier, as LoginIT does for the escalation: what matters
        // here is the attempt that crosses it, not the ones before.
        redisTemplate.opsForValue()
            .set(RedisKeySpace.forLoginAttempts(EMAIL), String.valueOf(FIRST_TIER_FAILURES - 1), ATTEMPT_WINDOW);

        accountLockedNotifier.failNextSend();

        // A 503 here would be visible only on the attempt that locks the account, which hands an
        // attacker the exact threshold. The notification is secondary; the denial is not.
        login(EMAIL, WRONG_PASSWORD, CAPTCHA_TOKEN)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_CREDENTIALS_CODE));

        // Nothing was delivered, which is what proves the outage happened at all: without this the
        // case would stay green on a notification that succeeded, asserting an absence of nothing.
        assertThat(accountLockedNotifier.capturedNothing()).isTrue();

        // And the lockout still applied: swallowing the failure must not swallow the operation.
        String lockoutKey = RedisKeySpace.forLoginLockout(EMAIL);

        assertThat(redisTemplate.opsForValue().get(lockoutKey))
            .isEqualTo(RedisKeySpace.LOGIN_LOCKOUT_VALUE);

        assertThat(redisTemplate.getExpire(lockoutKey))
            .isBetween(1L, FIRST_TIER_LOCKOUT.toSeconds());
    }

    @Test
    void resendActivationCode_smtpDown_failsRetryable() {

        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        accountActivationNotifier.failNextSend();

        Response resendResponse = resendActivationCode(EMAIL);

        // The 503 lands only on this branch — unknown and already-active answer 204 — so an outage
        // tells a registered address from an unknown one. Kept: register already answers 409 for an
        // active email, and hiding it would cost a real caller the signal that their code never left.
        assertOpaqueInfrastructureFailure(resendResponse);
    }

    @Test
    void resendActivationCode_afterSmtpRecovers_deliversAUsableCode() {

        accountActivationNotifier.failNextSend();

        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value());

        // The other half of the policy: the pending account left behind is not a dead end. The
        // switch is one-shot, so this send is the recovered one.
        resendActivationCode(EMAIL)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String resentOtp = activationOtpFor(EMAIL);

        activate(EMAIL, resentOtp)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    void initiatePasswordReset_smtpDown_failsRetryable() {

        registerAndActivate(EMAIL, PASSWORD);

        passwordResetNotifier.failNextSend();

        Response initiateResponse = initiatePasswordReset(EMAIL);

        // Same trade-off as the resend above, on the flow that reaches an active account.
        assertOpaqueInfrastructureFailure(initiateResponse);
    }

    private AccountStatus accountStatusOf(String email) {

        return accountJpaRepository.findByEmail(email)
            .orElseThrow()
            .getStatus();
    }
}
