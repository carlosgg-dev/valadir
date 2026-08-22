package com.valadir.common.error;

import com.valadir.common.error.ErrorCode.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    void getCode_allErrorCodes_areUnique() {

        var codes = Arrays.stream(ErrorCode.values())
            .map(ErrorCode::getCode)
            .toList();

        assertThat(codes).doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @CsvSource({
        "INVALID_FIELD,                            VAL-001,   VALIDATION",
        "INVALID_PASSWORD,                         VAL-002,   VALIDATION",
        "REQUIRED_FIELD_MISSING,                   VAL-003,   VALIDATION",
        "INVALID_OTP,                              VAL-004,   VALIDATION",
        "INSECURE_PASSWORD,                        BIZ-001,   VALIDATION",
        "EMAIL_ALREADY_EXISTS,                     BIZ-002,   CONFLICT",
        "AUTHENTICATION_FAILED,                    BIZ-003,   UNAUTHORIZED",
        "ACCOUNT_PENDING_ACTIVATION,               BIZ-004,   FORBIDDEN",
        "INVALID_ACCOUNT_ACTIVATION_OTP,           BIZ-005,   UNAUTHORIZED",
        "INVALID_PASSWORD_RESET_OTP,               BIZ-006,   UNAUTHORIZED",
        "INVALID_PASSWORD_RESET_VERIFICATION_TOKEN,BIZ-007,   UNAUTHORIZED",
        "CREDENTIAL_INTEGRITY_ERROR,               SEC-001,   UNAUTHORIZED",
        "INVALID_TOKEN,                            SEC-002,   UNAUTHORIZED",
        "AUTHENTICATION_REQUIRED,                  SEC-003,   UNAUTHORIZED",
        "ACCESS_DENIED,                            SEC-004,   FORBIDDEN",
        "RATE_LIMIT_EXCEEDED,                      SEC-005,   RATE_LIMITED",
        "ACCOUNT_TEMPORARILY_LOCKED,               SEC-006,   RATE_LIMITED",
        "CAPTCHA_REQUIRED,                         SEC-007,   FORBIDDEN",
        "MALFORMED_REQUEST,                        REQ-001,   VALIDATION",
        "INFRASTRUCTURE_UNAVAILABLE,               INFRA-001, SERVER_ERROR",
        "DATA_INTEGRITY_ERROR,                     PER-001,   SERVER_ERROR",
        "INTERNAL_SERVER_ERROR,                    SYS-001,   SERVER_ERROR"
    })
    void errorCode_everyConstant_mapsToExpectedCodeAndCategory(ErrorCode errorCode, String expectedCode, Category expectedCategory) {

        assertThat(errorCode.getCode()).isEqualTo(expectedCode);
        assertThat(errorCode.getCategory()).isEqualTo(expectedCategory);
    }

    @Test
    void values_everyConstant_isCoveredByMappingTest() {

        assertThat(ErrorCode.values()).hasSize(22);
    }
}
