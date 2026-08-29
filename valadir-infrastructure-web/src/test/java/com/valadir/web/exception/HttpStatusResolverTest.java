package com.valadir.web.exception;

import com.valadir.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class HttpStatusResolverTest {

    private final HttpStatusResolver resolver = new HttpStatusResolver();

    @ParameterizedTest
    @CsvSource({
        "INVALID_FIELD,                            BAD_REQUEST",
        "INVALID_PASSWORD,                         BAD_REQUEST",
        "REQUIRED_FIELD_MISSING,                   BAD_REQUEST",
        "INVALID_OTP,                              BAD_REQUEST",
        "INSECURE_PASSWORD,                        BAD_REQUEST",
        "MALFORMED_REQUEST,                        BAD_REQUEST",
        "EMAIL_ALREADY_EXISTS,                     CONFLICT",
        "INVALID_ACCOUNT_ACTIVATION_OTP,           UNAUTHORIZED",
        "INVALID_PASSWORD_RESET_OTP,               UNAUTHORIZED",
        "INVALID_PASSWORD_RESET_VERIFICATION_TOKEN,UNAUTHORIZED",
        "CREDENTIAL_INTEGRITY_ERROR,               UNAUTHORIZED",
        "INVALID_TOKEN,                            UNAUTHORIZED",
        "AUTHENTICATION_REQUIRED,                  UNAUTHORIZED",
        "ACCOUNT_PENDING_ACTIVATION,               FORBIDDEN",
        "ACCESS_DENIED,                            FORBIDDEN",
        "CAPTCHA_REQUIRED,                         FORBIDDEN",
        "RATE_LIMIT_EXCEEDED,                      TOO_MANY_REQUESTS",
        "ACCOUNT_TEMPORARILY_LOCKED,               TOO_MANY_REQUESTS",
        "INFRASTRUCTURE_UNAVAILABLE,               SERVICE_UNAVAILABLE",
        "DATA_INTEGRITY_ERROR,                     INTERNAL_SERVER_ERROR",
        "INTERNAL_SERVER_ERROR,                    INTERNAL_SERVER_ERROR"
    })
    void resolve_everyErrorCode_returnsExpectedStatus(ErrorCode errorCode, HttpStatus expectedStatus) {

        assertThat(resolver.resolve(errorCode)).isEqualTo(expectedStatus);
    }

    @Test
    void values_everyErrorCode_isCoveredByStatusTest() {

        assertThat(ErrorCode.values()).hasSize(21);
    }
}
