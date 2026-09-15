package com.valadir.e2e;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.Language;
import com.valadir.web.config.ApiRoutes;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

class PasswordChangeIT extends AbstractAuthE2EIT {

    private static final String EMAIL = "bruce.wayne@email.com";
    private static final String BYSTANDER_EMAIL = "clark.kent@email.com";
    private static final String PASSWORD = "SecureP@ss123";
    private static final String NEW_PASSWORD = "AnotherP@ss456";
    private static final String WRONG_PASSWORD = "Wrong@password123";

    // Well-formed, so it gets past the RawPassword policy and is refused for carrying the registered full name
    private static final String PERSONAL_DATA_PASSWORD = "BruceWayne@1";

    // Mirrors auth.lockout: the threshold past which a login is challenged.
    private static final int CHALLENGE_THRESHOLD = 3;

    @Test
    void changePassword_correctCurrentPassword_newPasswordSignsInAndOldDoesNot() {

        registerAndActivate(EMAIL, PASSWORD);

        changePassword(accessTokenOf(login(EMAIL, PASSWORD)), PASSWORD, NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode()));

        login(EMAIL, NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    void changePassword_liveSessionsOnTwoDevices_stopAuthenticating() {

        registerAndActivate(EMAIL, PASSWORD);

        Response callingDeviceLogin = login(EMAIL, PASSWORD);
        String callingDeviceAccessToken = accessTokenOf(callingDeviceLogin);
        String callingDeviceRefreshToken = refreshTokenOf(callingDeviceLogin);

        Response otherDeviceLogin = login(EMAIL, PASSWORD);
        String otherDeviceAccessToken = accessTokenOf(otherDeviceLogin);
        String otherDeviceRefreshToken = refreshTokenOf(otherDeviceLogin);

        changePassword(callingDeviceAccessToken, PASSWORD, NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        // Revoking the refresh tokens alone would leave this access token authenticating for the rest of its
        // lifetime — on the very device a password change is meant to shut out.
        logoutAll(otherDeviceAccessToken)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.AUTHENTICATION_REQUIRED.getCode()));

        refresh(otherDeviceRefreshToken)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.INVALID_TOKEN.getCode()));

        // The caller is signed out with the rest: the 204 hands out no new pair to replace this one.
        refresh(callingDeviceRefreshToken)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.INVALID_TOKEN.getCode()));
    }

    @Test
    void changePassword_twoAccounts_leavesTheOtherAccountsSessionsAndPasswordIntact() {

        registerAndActivate(EMAIL, PASSWORD);
        registerAndActivate(BYSTANDER_EMAIL, PASSWORD);

        String callingAccessToken = accessTokenOf(login(EMAIL, PASSWORD));

        // The bystander never calls: its session and its password, the same as the caller's, are what the
        // change must not reach.
        Response bystanderLogin = login(BYSTANDER_EMAIL, PASSWORD);
        String bystanderAccessToken = accessTokenOf(bystanderLogin);
        String bystanderRefreshToken = refreshTokenOf(bystanderLogin);
        String bystanderAccountId = accountIdFor(BYSTANDER_EMAIL);

        changePassword(callingAccessToken, PASSWORD, NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(sessionFingerprintsFor(bystanderAccountId)).containsExactly(fingerprintOf(bystanderRefreshToken));

        // Minted before the change: a cutoff written to the bystander's account would refuse it.
        logoutAll(bystanderAccessToken)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        login(BYSTANDER_EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    void changePassword_wrongCurrentPassword_returns401AndKeepsThePassword() {

        registerAndActivate(EMAIL, PASSWORD);

        changePassword(accessTokenOf(login(EMAIL, PASSWORD)), WRONG_PASSWORD, NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode()));

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    // Answering the new password's policy first would leave the wrong guess uncounted
    @Test
    void changePassword_wrongCurrentPasswordWithNewPasswordFailingPolicy_returns401AndCountsTheFailure() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        changePassword(accessTokenOf(loggedIn), WRONG_PASSWORD, "weak")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode()));

        assertThat(failedLoginAttemptsFor(EMAIL)).isEqualTo("1");
    }

    // Every link is unit-tested against a mock of the next: only the running application proves the
    // personal-data rule is actually reached.
    @Test
    void changePassword_newPasswordWithPersonalData_returns400AndKeepsTheOldPassword() {

        registerAndActivate(EMAIL, PASSWORD);

        changePassword(accessTokenOf(login(EMAIL, PASSWORD)), PASSWORD, PERSONAL_DATA_PASSWORD)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo(ErrorCode.INSECURE_PASSWORD.getCode()))
            .body("errors", nullValue());

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    void changePassword_afterFailedAttempts_clearsTheCounter() {

        registerAndActivate(EMAIL, PASSWORD);

        String accessToken = accessTokenOf(login(EMAIL, PASSWORD));

        // Enough failures to trip the challenge, short of the lockout that would deny the change itself.
        failPasswordChangeTimes(accessToken, CHALLENGE_THRESHOLD);

        changePassword(accessToken, PASSWORD, NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        // Failures counted against the old password would otherwise open the new one on a CAPTCHA challenge.
        login(EMAIL, NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    // The register step sends no Accept-Language header, so the account is in English.
    @Test
    void changePassword_succeeded_notifiesTheOwner() {

        registerAndActivate(EMAIL, PASSWORD);

        changePassword(accessTokenOf(login(EMAIL, PASSWORD)), PASSWORD, NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(passwordChangedNotifier.lastLanguageFor(EMAIL)).contains(Language.EN);
    }

    @Test
    void changePassword_withoutBearerToken_returns401AndKeepsThePassword() {

        registerAndActivate(EMAIL, PASSWORD);

        changePassword(null, PASSWORD, NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.AUTHENTICATION_REQUIRED.getCode()));

        login(EMAIL, PASSWORD)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    private Response changePassword(String accessToken, String currentPassword, String newPassword) {

        var request = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "currentPassword", currentPassword,
                "newPassword", newPassword
            ));

        if (accessToken != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }

        return request
            .when()
            .post(ApiRoutes.Auth.Account.CHANGE_PASSWORD_PATH);
    }

    private void failPasswordChangeTimes(String accessToken, int times) {

        IntStream.range(0, times).forEach(i -> changePassword(accessToken, WRONG_PASSWORD, NEW_PASSWORD)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode())));
    }
}
