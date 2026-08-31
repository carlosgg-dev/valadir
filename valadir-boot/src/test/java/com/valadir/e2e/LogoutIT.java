package com.valadir.e2e;

import com.valadir.common.error.ErrorCode;
import com.valadir.security.redis.RedisKeySpace;
import com.valadir.test.mother.PasswordMother;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

class LogoutIT extends AbstractAuthE2EIT {

    private static final String EMAIL = "bruce.wayne@email.com";
    private static final String PASSWORD = PasswordMother.raw().value();
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    // The delegate without the blacklist check: reading a jti must not go through the component
    // whose effect these tests assert, and must keep working on an already revoked token.
    @Autowired
    @Qualifier("nimbusJwtDecoder")
    private JwtDecoder jwtDecoder;

    @Test
    void logout_authenticatedSession_returns204AndInvalidatesBothTokens() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String accessToken = accessTokenOf(loggedIn);
        String refreshToken = refreshTokenOf(loggedIn);
        String jti = jtiOf(accessToken);

        logout(accessToken, refreshToken)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String blacklistKey = RedisKeySpace.forBlacklist(jti);

        assertThat(redisTemplate.opsForValue().get(blacklistKey))
            .isEqualTo(RedisKeySpace.BLACKLIST_REVOKED_VALUE);

        // The entry only has to outlive the token it revokes: without the TTL the blacklist would
        // grow forever, holding jtis that expired on their own long ago
        assertThat(redisTemplate.getExpire(blacklistKey))
            .isBetween(1L, ACCESS_TOKEN_TTL.toSeconds());

        String accountId = accountIdFor(EMAIL);

        // Revoking the access token alone would leave the session mintable again through /refresh
        assertThat(redisTemplate.opsForValue().get(refreshTokenKeyOf(refreshToken))).isNull();
        assertThat(sessionFingerprintsFor(accountId)).isEmpty();
    }

    @Test
    void logout_revokedAccessToken_isRejectedOnTheNextRequest() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String accessToken = accessTokenOf(loggedIn);
        String refreshToken = refreshTokenOf(loggedIn);

        logout(accessToken, refreshToken)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        // Writing the blacklist is worthless if nobody reads it: the token is still signed and
        // unexpired, so only the revocation check can stop it from authenticating again.
        logout(accessToken, refreshToken)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.AUTHENTICATION_REQUIRED.getCode()))
            .body("errors", nullValue());
    }

    @Test
    void logout_oneOfTwoSessions_revokesOnlyThatSession() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggingOutDeviceLogin = login(EMAIL, PASSWORD);
        String loggingOutDeviceAccessToken = accessTokenOf(loggingOutDeviceLogin);
        String loggingOutDeviceRefreshToken = refreshTokenOf(loggingOutDeviceLogin);

        Response bystanderDeviceLogin = login(EMAIL, PASSWORD);
        String bystanderDeviceAccessToken = accessTokenOf(bystanderDeviceLogin);
        String bystanderDeviceRefreshToken = refreshTokenOf(bystanderDeviceLogin);

        String accountId = accountIdFor(EMAIL);

        // Precondition: the account really holds two live sessions
        assertThat(sessionFingerprintsFor(accountId))
            .containsExactlyInAnyOrder(fingerprintOf(loggingOutDeviceRefreshToken), fingerprintOf(bystanderDeviceRefreshToken));

        logout(loggingOutDeviceAccessToken, loggingOutDeviceRefreshToken)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(sessionFingerprintsFor(accountId)).containsExactly(fingerprintOf(bystanderDeviceRefreshToken));
        assertThat(redisTemplate.opsForValue().get(refreshTokenKeyOf(bystanderDeviceRefreshToken)))
            .isNotNull();

        // Signing out of one device must not sign out of the rest: the revocation targets the jti
        // of the token presented, not every token the account holds.
        logout(bystanderDeviceAccessToken, bystanderDeviceRefreshToken)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void logout_withoutBearerToken_returns401AndLeavesSessionIntact() {

        registerAndActivate(EMAIL, PASSWORD);

        Response loggedIn = login(EMAIL, PASSWORD);
        String refreshToken = refreshTokenOf(loggedIn);

        logout(refreshToken)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ErrorCode.AUTHENTICATION_REQUIRED.getCode()))
            .body("errors", nullValue());

        String accountId = accountIdFor(EMAIL);

        // Logout is the only POST route outside POST_PUBLIC_ROUTES: an anonymous call must die in
        // the filter chain, before the refresh token named in the body can be revoked by a stranger
        assertThat(redisTemplate.opsForValue().get(refreshTokenKeyOf(refreshToken))).isNotNull();
        assertThat(sessionFingerprintsFor(accountId)).containsExactly(fingerprintOf(refreshToken));
        assertThat(redisTemplate.keys(RedisKeySpace.forBlacklist("*"))).isEmpty();
    }

    private String jtiOf(String accessToken) {

        return jwtDecoder.decode(accessToken).getId();
    }
}
