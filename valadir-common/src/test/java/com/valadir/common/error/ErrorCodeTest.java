package com.valadir.common.error;

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
        "INVALID_FIELD,                            VAL-001",
        "INVALID_PASSWORD,                         VAL-002",
        "REQUIRED_FIELD_MISSING,                   VAL-003",
        "INVALID_OTP,                              VAL-004",
        "INSECURE_PASSWORD,                        BIZ-001",
        "EMAIL_ALREADY_EXISTS,                     BIZ-002",
        "ACCOUNT_PENDING_ACTIVATION,               BIZ-003",
        "INVALID_ACCOUNT_ACTIVATION_OTP,           BIZ-004",
        "INVALID_PASSWORD_RESET_OTP,               BIZ-005",
        "INVALID_PASSWORD_RESET_VERIFICATION_TOKEN,BIZ-006",
        "CREDENTIAL_INTEGRITY_ERROR,               SEC-001",
        "INVALID_TOKEN,                            SEC-002",
        "AUTHENTICATION_REQUIRED,                  SEC-003",
        "ACCESS_DENIED,                            SEC-004",
        "RATE_LIMIT_EXCEEDED,                      SEC-005",
        "ACCOUNT_TEMPORARILY_LOCKED,               SEC-006",
        "CAPTCHA_REQUIRED,                         SEC-007",
        "MALFORMED_REQUEST,                        REQ-001",
        "INFRASTRUCTURE_UNAVAILABLE,               INFRA-001",
        "DATA_INTEGRITY_ERROR,                     PER-001",
        "INTERNAL_SERVER_ERROR,                    SYS-001"
    })
    void getCode_everyConstant_mapsToExpectedCode(ErrorCode errorCode, String expectedCode) {

        assertThat(errorCode.getCode()).isEqualTo(expectedCode);
    }

    @Test
    void values_everyConstant_isCoveredByMappingTest() {

        assertThat(ErrorCode.values()).hasSize(21);
    }
}
