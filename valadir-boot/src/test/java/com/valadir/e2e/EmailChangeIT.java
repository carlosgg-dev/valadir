package com.valadir.e2e;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.Language;
import com.valadir.security.redis.RedisKeySpace;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class EmailChangeIT extends AbstractAuthE2EIT {

    private static final String EMAIL = "bruce.wayne@email.com";
    private static final String BYSTANDER_EMAIL = "clark.kent@email.com";
    private static final String NEW_EMAIL = "matches.malone@email.com";
    private static final String OTHER_NEW_EMAIL = "the.batman@email.com";
    private static final String PASSWORD = "SecureP@ss123";
    private static final String OTHER_PASSWORD = "AnotherP@ss456";
    private static final String WRONG_PASSWORD = "Wrong@password123";

    // Mirrors auth.lockout: the threshold past which a login is challenged.
    private static final int CHALLENGE_THRESHOLD = 3;

    // Mirrors auth.email-change.otp.ttl. application-test.yml does not redeclare it, so this pins the production
    // binding from application.yml.
    private static final Duration OTP_TTL = Duration.ofMinutes(15);

    @Test
    void initiateEmailChange_correctPassword_sendsTheCodeToTheNewEmailAndKeepsTheOldOneUntilCompleted() {

        registerAndActivate(EMAIL, PASSWORD);

        initiateEmailChange(accessTokenOf(login(EMAIL, PASSWORD)), NEW_EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String requestKey = RedisKeySpace.forEmailChangeRequest(accountIdFor(EMAIL));
        String deliveredOtp = emailChangeOtpFor(NEW_EMAIL);

        // What Redis holds is a hash, not the plaintext code
        assertThat(redisTemplate.<String, String>opsForHash().values(requestKey))
            .isNotEmpty()
            .noneMatch(stored -> stored.contains(deliveredOtp));

        assertThat(redisTemplate.getExpire(requestKey))
            .isBetween(OTP_TTL.minusMinutes(1).toSeconds(), OTP_TTL.toSeconds());

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());

        login(NEW_EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode()));
    }

    @Test
    void initiateEmailChange_secondRequest_invalidatesTheFirstCode() {

        registerAndActivate(EMAIL, PASSWORD);

        String accessToken = accessTokenOf(login(EMAIL, PASSWORD));

        initiateEmailChange(accessToken, NEW_EMAIL, PASSWORD);
        String firstCode = emailChangeOtpFor(NEW_EMAIL);

        initiateEmailChange(accessToken, OTHER_NEW_EMAIL, PASSWORD);
        String secondCode = emailChangeOtpFor(OTHER_NEW_EMAIL);

        completeEmailChange(accessToken, firstCode)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.INVALID_EMAIL_CHANGE_OTP.getCode()));

        completeEmailChange(accessToken, secondCode)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        login(OTHER_NEW_EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    void initiateEmailChange_emailOfAnActiveAccount_returns409AndSendsNothing() {

        registerAndActivate(EMAIL, PASSWORD);
        registerAndActivate(BYSTANDER_EMAIL, PASSWORD);

        initiateEmailChange(accessTokenOf(login(EMAIL, PASSWORD)), BYSTANDER_EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.CONFLICT.value())
            .body("code", equalTo(ErrorCode.EMAIL_ALREADY_EXISTS.getCode()));

        assertThat(emailChangeNotifier.capturedNothing()).isTrue();
    }

    @Test
    void initiateEmailChange_wrongPassword_returns401AndSendsNothing() {

        registerAndActivate(EMAIL, PASSWORD);

        initiateEmailChange(accessTokenOf(login(EMAIL, PASSWORD)), NEW_EMAIL, WRONG_PASSWORD)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode()));

        assertThat(emailChangeNotifier.capturedNothing()).isTrue();
    }

    @Test
    void initiateEmailChange_withoutBearerToken_returns401AndSendsNothing() {

        registerAndActivate(EMAIL, PASSWORD);

        initiateEmailChange(null, NEW_EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.AUTHENTICATION_REQUIRED.getCode()));

        assertThat(emailChangeNotifier.capturedNothing()).isTrue();
    }

    // The register step sends no Accept-Language header, so the account is in English.
    @Test
    void completeEmailChange_validCode_signsInWithTheNewEmailOnlyAndNotifiesTheOldOne() {

        registerAndActivate(EMAIL, PASSWORD);

        String accessToken = accessTokenOf(login(EMAIL, PASSWORD));
        initiateEmailChange(accessToken, NEW_EMAIL, PASSWORD);

        completeEmailChange(accessToken, emailChangeOtpFor(NEW_EMAIL))
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        login(NEW_EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode()));

        // The alert goes to the address that lost the account, never to the one that took it
        assertThat(emailChangedNotifier.lastLanguageFor(EMAIL)).contains(Language.EN);
        assertThat(emailChangedNotifier.lastLanguageFor(NEW_EMAIL)).isEmpty();
    }

    // Sessions are keyed by account, not by address: the change proves the new mailbox and revokes no credential.
    @Test
    void completeEmailChange_liveSession_keepsAuthenticating() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String accessToken = accessTokenOf(loggedIn);
        String refreshToken = refreshTokenOf(loggedIn);

        initiateEmailChange(accessToken, NEW_EMAIL, PASSWORD);

        completeEmailChange(accessToken, emailChangeOtpFor(NEW_EMAIL))
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        refresh(refreshToken)
            .then()
            .statusCode(HttpStatus.OK.value());

        logoutAll(accessToken)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void completeEmailChange_replayedCode_isRejected() {

        registerAndActivate(EMAIL, PASSWORD);

        String accessToken = accessTokenOf(login(EMAIL, PASSWORD));
        initiateEmailChange(accessToken, NEW_EMAIL, PASSWORD);
        String code = emailChangeOtpFor(NEW_EMAIL);

        completeEmailChange(accessToken, code)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        completeEmailChange(accessToken, code)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.INVALID_EMAIL_CHANGE_OTP.getCode()));
    }

    @Test
    void completeEmailChange_wrongCode_returns401AndKeepsTheEmail() {

        registerAndActivate(EMAIL, PASSWORD);

        String accessToken = accessTokenOf(login(EMAIL, PASSWORD));
        initiateEmailChange(accessToken, NEW_EMAIL, PASSWORD);

        completeEmailChange(accessToken, otherOtpThan(emailChangeOtpFor(NEW_EMAIL)))
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.INVALID_EMAIL_CHANGE_OTP.getCode()));

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    // The request is looked up by the account of the token: a code carries no account of its own to point elsewhere.
    @Test
    void completeEmailChange_codeOfAnotherAccount_isRejected() {

        registerAndActivate(EMAIL, PASSWORD);
        registerAndActivate(BYSTANDER_EMAIL, PASSWORD);

        String accessToken = accessTokenOf(login(EMAIL, PASSWORD));
        String bystanderAccessToken = accessTokenOf(login(BYSTANDER_EMAIL, PASSWORD));

        initiateEmailChange(accessToken, NEW_EMAIL, PASSWORD);
        String code = emailChangeOtpFor(NEW_EMAIL);

        completeEmailChange(bystanderAccessToken, code)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.INVALID_EMAIL_CHANGE_OTP.getCode()));

        login(BYSTANDER_EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());

        // Nor did the attempt spend it: the owner still completes with the same code
        completeEmailChange(accessToken, code)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    // A pending account only claimed the address: whoever proves it takes it over, as a re-registration would.
    @Test
    void completeEmailChange_emailHeldByAPendingAccount_replacesIt() {

        registerAndActivate(EMAIL, PASSWORD);
        register(NEW_EMAIL, OTHER_PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        String accountId = accountIdFor(EMAIL);
        UUID pendingAccountId = UUID.fromString(accountIdFor(NEW_EMAIL));
        String accessToken = accessTokenOf(login(EMAIL, PASSWORD));

        initiateEmailChange(accessToken, NEW_EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        completeEmailChange(accessToken, emailChangeOtpFor(NEW_EMAIL))
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(accountIdFor(NEW_EMAIL)).isEqualTo(accountId);
        assertThat(accountJpaRepository.findById(pendingAccountId)).isEmpty();
        assertThat(userJpaRepository.findByAccountId(pendingAccountId)).isEmpty();

        login(NEW_EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    void completeEmailChange_emailActivatedByAnotherAccountMeanwhile_returns409AndKeepsTheEmail() {

        registerAndActivate(EMAIL, PASSWORD);
        register(NEW_EMAIL, OTHER_PASSWORD)
            .then()
            .statusCode(HttpStatus.CREATED.value());

        String accessToken = accessTokenOf(login(EMAIL, PASSWORD));
        initiateEmailChange(accessToken, NEW_EMAIL, PASSWORD);

        // Free to take at initiation, proved by its pending holder before the owner completes
        activate(NEW_EMAIL, activationOtpFor(NEW_EMAIL))
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        completeEmailChange(accessToken, emailChangeOtpFor(NEW_EMAIL))
            .then()
            .statusCode(HttpStatus.CONFLICT.value())
            .body("code", equalTo(ErrorCode.EMAIL_ALREADY_EXISTS.getCode()));

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());

        login(NEW_EMAIL, OTHER_PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    void completeEmailChange_afterFailedAttempts_letsTheOldEmailRegisterAndSignInAfresh() {

        registerAndActivate(EMAIL, PASSWORD);

        String accessToken = accessTokenOf(login(EMAIL, PASSWORD));

        // Enough failures to trip the challenge, short of the lockout that would deny the change itself.
        failEmailChangeInitiationTimes(accessToken, CHALLENGE_THRESHOLD);

        initiateEmailChange(accessToken, NEW_EMAIL, PASSWORD);

        completeEmailChange(accessToken, emailChangeOtpFor(NEW_EMAIL))
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        // Failures counted against the old address would otherwise open its next account on a CAPTCHA challenge.
        registerAndActivate(EMAIL, OTHER_PASSWORD);

        login(EMAIL, OTHER_PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    void completeEmailChange_withoutBearerToken_returns401AndKeepsTheEmail() {

        registerAndActivate(EMAIL, PASSWORD);

        initiateEmailChange(accessTokenOf(login(EMAIL, PASSWORD)), NEW_EMAIL, PASSWORD);

        completeEmailChange(null, emailChangeOtpFor(NEW_EMAIL))
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.AUTHENTICATION_REQUIRED.getCode()));

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    private void failEmailChangeInitiationTimes(String accessToken, int times) {

        IntStream.range(0, times).forEach(i -> initiateEmailChange(accessToken, NEW_EMAIL, WRONG_PASSWORD)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode())));
    }
}
