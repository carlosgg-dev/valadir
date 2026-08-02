package com.valadir.e2e;

import com.valadir.domain.model.AccountStatus;
import com.valadir.security.redis.RedisKeySpace;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

class AccountRegistrationIT extends AbstractAuthE2EIT {

    private static final String EMAIL = "bruce.wayne@email.com";
    private static final String SECOND_EMAIL = "clark.kent@email.com";
    private static final String UNKNOWN_EMAIL = "unknown@email.test";
    private static final String PASSWORD = "SecureP@ss123";

    // The same address as EMAIL, told apart only by case
    private static final String MIXED_CASE_EMAIL = "Bruce.Wayne@Email.com";
    private static final String UPPERCASE_EMAIL = "BRUCE.WAYNE@EMAIL.COM";

    // Passes @Email, rejected by Email.from
    private static final String EMAIL_WITHOUT_DOT_IN_DOMAIN = "bruce.wayne@email";

    // Rejected before hashing: fails the RawPassword policy
    private static final String TOO_SHORT_PASSWORD = "Short1@";
    // Rejected before hashing: contains the full name
    private static final String PERSONAL_DATA_PASSWORD = "BruceWayne@1";

    private static final String EMAIL_ALREADY_EXISTS_CODE = "BIZ-002";
    private static final String INVALID_ACTIVATION_OTP_CODE = "BIZ-005";
    private static final String INSECURE_PASSWORD_CODE = "BIZ-001";
    private static final String INVALID_PASSWORD_CODE = "VAL-002";
    private static final String INVALID_FIELD_CODE = "VAL-001";

    // Mirrors auth.account-activation.otp.ttl. Unlike the JWT TTLs, application-test.yml does not
    // redeclare it, so this pins the production binding from application.yml.
    private static final Duration ACTIVATION_OTP_TTL = Duration.ofMinutes(15);

    @Test
    void register_newEmail_returns201WithPendingAccountAndStoredOtp() {

        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        String accountId = accountIdOf(EMAIL);

        assertThat(accountStatusOf(EMAIL)).isEqualTo(AccountStatus.PENDING_ACTIVATION);

        // The profile travels request → command → domain → entity; only an E2E sees it land intact
        var user = userJpaRepository.findByAccountId(UUID.fromString(accountId)).orElseThrow();
        assertThat(user.getFullName()).isEqualTo(FULL_NAME);
        assertThat(user.getGivenName()).isEqualTo(GIVEN_NAME);

        String otpKey = RedisKeySpace.forAccountActivationOtp(accountId);
        String deliveredOtp = activationOtpFor(EMAIL);

        // What Redis holds is a hash: storing the code as sent would turn a Redis dump into a set
        // of working activation codes, and every other test would stay green.
        assertThat(redisTemplate.opsForValue().get(otpKey))
            .isNotNull()
            .doesNotContain(deliveredOtp);

        assertThat(redisTemplate.getExpire(otpKey))
            .isBetween(ACTIVATION_OTP_TTL.minusMinutes(1).toSeconds(), ACTIVATION_OTP_TTL.toSeconds());
    }

    @Test
    void register_emailPassingBeanValidationButRejectedByDomain_returns400WithoutFieldErrors() {

        // Jakarta's @Email accepts a domain without a dot, Email.from does not: this is the only
        // way in to the DomainException branch of the use case. The null errors array is what
        // tells it apart from the VAL-001 Bean Validation returns.
        register(EMAIL_WITHOUT_DOT_IN_DOMAIN, PASSWORD)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo(INVALID_FIELD_CODE))
            .body("errors", nullValue());

        assertThat(accountJpaRepository.count()).isZero();
    }

    @Test
    void register_alreadyActiveEmail_returns409AndSendsNoOtp() {

        registerAndActivate(EMAIL, PASSWORD);

        String accountId = accountIdOf(EMAIL);

        assertThat(accountIdOf(EMAIL)).isEqualTo(accountId);
        assertThat(accountStatusOf(EMAIL)).isEqualTo(AccountStatus.ACTIVE);

        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CONFLICT.value())
            .body("code", equalTo(EMAIL_ALREADY_EXISTS_CODE))
            .body("errors", nullValue());

        // A rejected registration must not mint a code for an account that is already live:
        // that would hand a stranger a fresh OTP for somebody else's mailbox.
        assertThat(redisTemplate.hasKey(RedisKeySpace.forAccountActivationOtp(accountId))).isFalse();
    }

    @Test
    void register_emailPendingActivation_replacesTheAccountAndInvalidatesTheOldOtp() {

        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        String abandonedAccountId = accountIdOf(EMAIL);
        String abandonedOtp = activationOtpFor(EMAIL);

        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        String currentAccountId = accountIdOf(EMAIL);
        String currentOtp = activationOtpFor(EMAIL);

        // The pending account is replaced, not duplicated: the unique email index would reject a
        // second row, so the rows of the abandoned attempt must be gone.
        assertThat(currentAccountId).isNotEqualTo(abandonedAccountId);
        assertThat(accountJpaRepository.count()).isEqualTo(1);
        assertThat(userJpaRepository.count()).isEqualTo(1);
        assertThat(userJpaRepository.findByAccountId(UUID.fromString(abandonedAccountId))).isEmpty();

        activate(EMAIL, abandonedOtp)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_ACTIVATION_OTP_CODE));

        assertThat(accountStatusOf(EMAIL)).isEqualTo(AccountStatus.PENDING_ACTIVATION);

        activate(EMAIL, currentOtp)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(accountStatusOf(EMAIL)).isEqualTo(AccountStatus.ACTIVE);
    }

    @ParameterizedTest
    @MethodSource("rejectedPasswords")
    void register_rejectedPassword_returns400AndPersistsNothing(String password, String expectedCode) {

        register(EMAIL, password)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo(expectedCode))
            .body("errors", nullValue());

        // Both guards live in the domain: if either stopped being wired into registration, a weak
        // password would be accepted here and no unit test would notice.
        assertThat(accountJpaRepository.findByEmail(EMAIL)).isEmpty();
        assertThat(accountActivationNotifier.lastOtpFor(EMAIL)).isEmpty();
    }

    @Test
    void register_concurrentSameEmail_createsExactlyOneAccount() throws Exception {

        List<Response> responses = registerConcurrently(EMAIL, PASSWORD);

        List<Response> created = responsesWithStatus(responses, HttpStatus.CREATED);
        List<Response> rejected = responsesWithStatus(responses, HttpStatus.CONFLICT);

        // Both requests find the email free and both insert; only the unique index tells them
        // apart. The rejected one must read as the conflict it is, not as a server error.
        assertThat(created).hasSize(1);
        assertThat(rejected).hasSize(1);

        rejected.getFirst().then().body("code", equalTo(EMAIL_ALREADY_EXISTS_CODE));

        assertThat(accountJpaRepository.count()).isEqualTo(1);
        assertThat(userJpaRepository.count()).isEqualTo(1);

        String activationOtp = activationOtpFor(EMAIL);

        activate(EMAIL, activationOtp)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void register_emailInAnotherCase_reachesTheSameAccount() {

        register(MIXED_CASE_EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        // Stored normalized: the row answers to a spelling the request never sent.
        assertThat(accountJpaRepository.findByEmail(EMAIL)).isPresent();

        String activationOtp = activationOtpFor(EMAIL);

        activate(UPPERCASE_EMAIL, activationOtp)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(accountStatusOf(EMAIL)).isEqualTo(AccountStatus.ACTIVE);

        // Registering it again in yet another case is a conflict, not a second account: without
        // normalization the unique index would take each spelling for a different address.
        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CONFLICT.value())
            .body("code", equalTo(EMAIL_ALREADY_EXISTS_CODE));

        assertThat(accountJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void activate_capturedOtp_returns204AndActivatesTheAccount() {

        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        String accountId = accountIdOf(EMAIL);
        String activationOtp = activationOtpFor(EMAIL);

        activate(EMAIL, activationOtp)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(accountStatusOf(EMAIL)).isEqualTo(AccountStatus.ACTIVE);

        // Single use: the code is spent on success, it does not sit around until its TTL
        assertThat(redisTemplate.hasKey(RedisKeySpace.forAccountActivationOtp(accountId))).isFalse();
    }

    @Test
    void activate_alreadyActivatedAccount_returns401OnTheReplayedOtp() {

        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        String activationOtp = activationOtpFor(EMAIL);

        activate(EMAIL, activationOtp)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        activate(EMAIL, activationOtp)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_ACTIVATION_OTP_CODE))
            .body("errors", nullValue());

        assertThat(accountStatusOf(EMAIL)).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void activate_unknownEmail_returns401WithoutRevealingTheAccountIsMissing() {

        activate(UNKNOWN_EMAIL, "123456")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_ACTIVATION_OTP_CODE))
            .body("errors", nullValue());

        assertThat(accountJpaRepository.count()).isZero();
    }

    @Test
    void activate_wrongOtp_returns401AndKeepsTheAccountPending() {

        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        String activationOtp = activationOtpFor(EMAIL);

        activate(EMAIL, otherOtpThan(activationOtp))
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_ACTIVATION_OTP_CODE))
            .body("errors", nullValue());

        assertThat(accountStatusOf(EMAIL)).isEqualTo(AccountStatus.PENDING_ACTIVATION);

        // A failed attempt must not burn the real code: otherwise anyone knowing the email could
        // lock the owner out of their own activation by guessing once.
        activate(EMAIL, activationOtp)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void activate_otpOfAnotherPendingAccount_returns401AndKeepsBothPending() {

        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        register(SECOND_EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        String otherAccountOtp = activationOtpFor(SECOND_EMAIL);

        // The OTP is looked up under the account resolved from the email presented: a code that is
        // valid somewhere else must not activate this one.
        activate(EMAIL, otherAccountOtp)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_ACTIVATION_OTP_CODE));

        assertThat(accountStatusOf(EMAIL)).isEqualTo(AccountStatus.PENDING_ACTIVATION);
        assertThat(accountStatusOf(SECOND_EMAIL)).isEqualTo(AccountStatus.PENDING_ACTIVATION);
    }

    @Test
    void activate_expiredOtp_returns401AndKeepsTheAccountPending() {

        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        String accountId = accountIdOf(EMAIL);
        String activationOtp = activationOtpFor(EMAIL);

        // What the TTL leaves behind: the account still pending, the code gone from Redis.
        redisTemplate.delete(RedisKeySpace.forAccountActivationOtp(accountId));

        // Even the code the owner really received stops working once it is no longer stored.
        activate(EMAIL, activationOtp)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_ACTIVATION_OTP_CODE))
            .body("errors", nullValue());

        assertThat(accountStatusOf(EMAIL)).isEqualTo(AccountStatus.PENDING_ACTIVATION);
    }

    @Test
    void resendActivationCode_pendingAccount_returns204AndReplacesTheOtp() {

        register(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        String accountId = accountIdOf(EMAIL);
        String otpKey = RedisKeySpace.forAccountActivationOtp(accountId);
        String supersededOtp = activationOtpFor(EMAIL);

        resendActivationCode(EMAIL)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String resentOtp = activationOtpFor(EMAIL);

        assertThat(resentOtp).isNotEqualTo(supersededOtp);
        assertThat(redisTemplate.getExpire(otpKey))
            .isBetween(ACTIVATION_OTP_TTL.minusMinutes(1).toSeconds(), ACTIVATION_OTP_TTL.toSeconds());

        // Resending replaces the code, it does not add a second one: an account never has two
        // live activation codes, so every resend shortens the window an old code is usable.
        activate(EMAIL, supersededOtp)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(INVALID_ACTIVATION_OTP_CODE));

        activate(EMAIL, resentOtp)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(accountStatusOf(EMAIL)).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void resendActivationCode_activeAccount_returns204AndSendsNothing() {

        registerAndActivate(EMAIL, PASSWORD);

        String accountId = accountIdOf(EMAIL);

        resendActivationCode(EMAIL)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        // Activation deleted the key; had the resend issued a code for a live account, it would be
        // back — and an activation flow would be reopened on an account that no longer needs one.
        assertThat(redisTemplate.hasKey(RedisKeySpace.forAccountActivationOtp(accountId))).isFalse();
    }

    @Test
    void resendActivationCode_unknownEmail_returns204AndSendsNothing() {

        // Same 204 a pending account gets: the response must not tell whether the email is
        // registered, and no mail may be sent to an address nobody signed up with.
        resendActivationCode(UNKNOWN_EMAIL)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(accountActivationNotifier.lastOtpFor(UNKNOWN_EMAIL)).isEmpty();
        assertThat(redisTemplate.keys(RedisKeySpace.forAccountActivationOtp("*"))).isEmpty();
    }

    private AccountStatus accountStatusOf(String email) {

        return accountJpaRepository.findByEmail(email)
            .orElseThrow()
            .getStatus();
    }

    private static Stream<Arguments> rejectedPasswords() {

        return Stream.of(
            Arguments.of(TOO_SHORT_PASSWORD, INVALID_PASSWORD_CODE),
            Arguments.of(PERSONAL_DATA_PASSWORD, INSECURE_PASSWORD_CODE)
        );
    }

    // Both threads wait on the latch and are released together, so the two registrations look up
    // the email before either of them has inserted it.
    private List<Response> registerConcurrently(String email, String password) throws Exception {

        var startLine = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {

            List<Future<Response>> pending = List.of(
                executor.submit(() -> awaitAndRegister(startLine, email, password)),
                executor.submit(() -> awaitAndRegister(startLine, email, password))
            );

            startLine.countDown();

            return List.of(
                pending.get(0).get(30, TimeUnit.SECONDS),
                pending.get(1).get(30, TimeUnit.SECONDS)
            );
        }
    }

    private Response awaitAndRegister(CountDownLatch startLine, String email, String password) throws InterruptedException {

        startLine.await();
        return register(email, password);
    }
}
