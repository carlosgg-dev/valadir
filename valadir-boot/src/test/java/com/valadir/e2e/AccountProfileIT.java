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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

class AccountProfileIT extends AbstractAuthE2EIT {

    private static final String EMAIL = "bruce.wayne@email.com";
    private static final String BYSTANDER_EMAIL = "clark.kent@email.com";
    private static final String PASSWORD = "SecureP@ss123";

    // The register step sends no Accept-Language header, so every account here starts in English.
    private static final String REGISTERED_LANGUAGE = "en";

    private static final String NEW_FULL_NAME = "Bruce Thomas Wayne";
    private static final String NEW_GIVEN_NAME = "Matches Malone";
    private static final String NEW_LANGUAGE = "es";

    // Both accounts register with the same names, so the email is what tells their profiles apart.
    @Test
    void getProfile_twoAccounts_returnsOnlyTheCallersProfile() {

        registerAndActivate(EMAIL, PASSWORD);
        registerAndActivate(BYSTANDER_EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        getProfile(accessTokenOf(loggedIn))
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("email", equalTo(EMAIL))
            .body("fullName", equalTo(FULL_NAME))
            .body("givenName", equalTo(GIVEN_NAME))
            .body("language", equalTo(REGISTERED_LANGUAGE));
    }

    @Test
    void getProfile_withoutBearerToken_returns401() {

        // Anonymous call against a protected route: no Authorization header.
        getProfile(null)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.AUTHENTICATION_REQUIRED.getCode()));
    }

    @Test
    void updateProfile_validBody_returns200WithTheStoredProfileAndGetReflectsIt() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String accessToken = accessTokenOf(loggedIn);

        updateProfile(accessToken, updateRequestBody(NEW_FULL_NAME, NEW_GIVEN_NAME, NEW_LANGUAGE))
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("email", equalTo(EMAIL))
            .body("fullName", equalTo(NEW_FULL_NAME))
            .body("givenName", equalTo(NEW_GIVEN_NAME))
            .body("language", equalTo(NEW_LANGUAGE));

        // The PUT answers from what it wrote; only a fresh read proves it was stored.
        getProfile(accessToken)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("email", equalTo(EMAIL))
            .body("fullName", equalTo(NEW_FULL_NAME))
            .body("givenName", equalTo(NEW_GIVEN_NAME))
            .body("language", equalTo(NEW_LANGUAGE));
    }

    @Test
    void updateProfile_nullGivenName_clearsIt() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String accessToken = accessTokenOf(loggedIn);

        updateProfile(accessToken, updateRequestBody(NEW_FULL_NAME, null, NEW_LANGUAGE))
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("givenName", nullValue());

        var storedUser = userJpaRepository.findByAccountId(UUID.fromString(accountIdFor(EMAIL))).orElseThrow();
        assertThat(storedUser.getGivenName()).isNull();
    }

    @Test
    void updateProfile_twoAccounts_leavesTheOtherProfileUntouched() {

        registerAndActivate(EMAIL, PASSWORD);
        registerAndActivate(BYSTANDER_EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        updateProfile(accessTokenOf(loggedIn), updateRequestBody(NEW_FULL_NAME, NEW_GIVEN_NAME, NEW_LANGUAGE))
            .then()
            .statusCode(HttpStatus.OK.value());

        var bystanderAccountId = UUID.fromString(accountIdFor(BYSTANDER_EMAIL));

        var bystanderUser = userJpaRepository.findByAccountId(bystanderAccountId).orElseThrow();
        assertThat(bystanderUser.getFullName()).isEqualTo(FULL_NAME);
        assertThat(bystanderUser.getGivenName()).isEqualTo(GIVEN_NAME);

        var bystanderAccount = accountJpaRepository.findById(bystanderAccountId).orElseThrow();
        assertThat(bystanderAccount.getLanguage()).isEqualTo(Language.EN);
    }

    // Well-formed and within Bean Validation, so it is the domain that must refuse a language we do not write.
    @Test
    void updateProfile_unsupportedLanguage_returns400AndKeepsTheProfile() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String accessToken = accessTokenOf(loggedIn);

        updateProfile(accessToken, updateRequestBody(NEW_FULL_NAME, NEW_GIVEN_NAME, "fr"))
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo(ErrorCode.INVALID_FIELD.getCode()));

        getProfile(accessToken)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("fullName", equalTo(FULL_NAME))
            .body("givenName", equalTo(GIVEN_NAME))
            .body("language", equalTo(REGISTERED_LANGUAGE));
    }

    // Passes JSON parsing and Bean Validation, so only the domain can refuse the NUL before it reaches the database
    @Test
    void updateProfile_fullNameWithControlCharacter_returns400AndKeepsTheProfile() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String accessToken = accessTokenOf(loggedIn);

        updateProfile(accessToken, updateRequestBody("Bruce\0Wayne", NEW_GIVEN_NAME, NEW_LANGUAGE))
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo(ErrorCode.INVALID_FIELD.getCode()));

        getProfile(accessToken)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("fullName", equalTo(FULL_NAME))
            .body("givenName", equalTo(GIVEN_NAME))
            .body("language", equalTo(REGISTERED_LANGUAGE));
    }

    // Two accounts: with one, a name that only differs by an invisible character is indistinguishable from nothing to compare it to
    @Test
    void updateProfile_fullNameWithNoBreakSpace_storesTheSameNameAsTheOtherAccount() {

        registerAndActivate(EMAIL, PASSWORD);
        registerAndActivate(BYSTANDER_EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String lookalikeOfTheBystandersName = FULL_NAME.replace(" ", " ");

        updateProfile(accessTokenOf(loggedIn), updateRequestBody(lookalikeOfTheBystandersName, NEW_GIVEN_NAME, NEW_LANGUAGE))
            .then()
            .statusCode(HttpStatus.OK.value());

        var caller = userJpaRepository.findByAccountId(UUID.fromString(accountIdFor(EMAIL))).orElseThrow();
        var bystander = userJpaRepository.findByAccountId(UUID.fromString(accountIdFor(BYSTANDER_EMAIL))).orElseThrow();

        assertThat(caller.getFullName()).isEqualTo(bystander.getFullName());
    }

    // Worse than the NUL above: Postgres refuses that one loudly, while the driver rewrites this one and answers success
    @Test
    void updateProfile_fullNameWithLoneSurrogate_returns400AndKeepsTheProfile() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String accessToken = accessTokenOf(loggedIn);

        // The escape sequence, never a raw surrogate: serializing one would rewrite it before the request left the test
        String bodyWithLoneSurrogate = String.format(Locale.ROOT, """
            {"fullName": "Bruce\\uD800Wayne", "givenName": "%s", "language": "%s"}
            """, NEW_GIVEN_NAME, NEW_LANGUAGE);

        updateProfile(accessToken, bodyWithLoneSurrogate)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo(ErrorCode.INVALID_FIELD.getCode()));

        getProfile(accessToken)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("fullName", equalTo(FULL_NAME))
            .body("givenName", equalTo(GIVEN_NAME))
            .body("language", equalTo(REGISTERED_LANGUAGE));
    }

    @Test
    void updateProfile_withoutBearerToken_returns401AndKeepsTheProfile() {

        registerAndActivate(EMAIL, PASSWORD);

        // Anonymous call against a protected route: no Authorization header.
        updateProfile(null, updateRequestBody(NEW_FULL_NAME, NEW_GIVEN_NAME, NEW_LANGUAGE))
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.AUTHENTICATION_REQUIRED.getCode()));

        var storedUser = userJpaRepository.findByAccountId(UUID.fromString(accountIdFor(EMAIL))).orElseThrow();
        assertThat(storedUser.getFullName()).isEqualTo(FULL_NAME);
    }

    private Response getProfile(String accessToken) {

        var request = RestAssured.given();

        if (accessToken != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }

        return request
            .when()
            .get(ApiRoutes.Auth.Account.PROFILE_PATH);
    }

    // Object, not Map: a body carrying a JSON escape is sent as it was written, without a serializer in between
    private Response updateProfile(String accessToken, Object body) {

        var request = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(body);

        if (accessToken != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }

        return request
            .when()
            .put(ApiRoutes.Auth.Account.PROFILE_PATH);
    }

    // Mutable: a null given name is one of the bodies under test, and Map.of rejects it.
    private static Map<String, String> updateRequestBody(String fullName, String givenName, String language) {

        Map<String, String> body = new HashMap<>();
        body.put("fullName", fullName);
        body.put("givenName", givenName);
        body.put("language", language);

        return body;
    }
}
