package com.valadir.web.exception;

import com.valadir.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class HttpStatusResolverTest {

    private final HttpStatusResolver resolver = new HttpStatusResolver();

    @Test
    void resolve_validationCategory_returns400() {

        assertThat(resolver.resolve(ErrorCode.INVALID_PASSWORD)).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resolve_conflictCategory_returns409() {

        assertThat(resolver.resolve(ErrorCode.EMAIL_ALREADY_EXISTS)).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void resolve_unauthorizedCategory_returns401() {

        assertThat(resolver.resolve(ErrorCode.CREDENTIAL_INTEGRITY_ERROR)).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void resolve_forbiddenCategory_returns403() {

        assertThat(resolver.resolve(ErrorCode.ACCESS_DENIED)).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void resolve_rateLimitedCategory_returns429() {

        assertThat(resolver.resolve(ErrorCode.RATE_LIMIT_EXCEEDED)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void resolve_serverErrorCategory_returns500() {

        assertThat(resolver.resolve(ErrorCode.TOKEN_REVOCATION_FAILED)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
