package com.valadir.web.exception;

import com.valadir.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeResolverTest {

    private final ErrorCodeResolver resolver = new ErrorCodeResolver();

    @Test
    void resolve_clientError_returnsMalformedRequest() {

        ErrorCode errorCode = resolver.resolve(HttpStatusCode.valueOf(400));

        assertThat(errorCode).isEqualTo(ErrorCode.MALFORMED_REQUEST);
    }

    @Test
    void resolve_serverError_returnsInternalServerError() {

        ErrorCode errorCode = resolver.resolve(HttpStatusCode.valueOf(500));

        assertThat(errorCode).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
