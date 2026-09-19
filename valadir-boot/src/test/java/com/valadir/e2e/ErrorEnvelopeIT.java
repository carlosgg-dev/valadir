package com.valadir.e2e;

import com.valadir.common.error.ErrorCode;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The 401 is written by {@code JwtAuthenticationEntryPoint} through {@code ErrorResponseWriter},
 * outside MVC, so nothing else pins the media type it answers with — and a servlet writer would
 * append the container's default charset to it. The 429 of the rate limit filter, the other path
 * through a writer of our own, belongs to {@link RateLimitEnforcementIT}.
 */
class ErrorEnvelopeIT extends AbstractAuthE2EIT {

    @Test
    void protectedEndpoint_withoutCredentials_answersJsonWithNoCharsetParameter() {

        Response response = RestAssured.get("/api/auth/account/profile");

        response.then().statusCode(HttpStatus.UNAUTHORIZED.value());

        assertThat(response.contentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.jsonPath().getMap("$"))
            .containsExactly(Map.entry("code", ErrorCode.AUTHENTICATION_REQUIRED.getCode()));
    }
}
