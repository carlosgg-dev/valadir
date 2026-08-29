package com.valadir.common.error;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @ParameterizedTest
    @CsvSource({
        "INVALID_FIELD,                            invalid_field",
        "INVALID_PASSWORD,                         invalid_password",
        "REQUIRED_FIELD_MISSING,                   required_field_missing",
        "INVALID_OTP,                              invalid_otp",
        "INSECURE_PASSWORD,                        insecure_password",
        "EMAIL_ALREADY_EXISTS,                     email_already_exists",
        "ACCOUNT_PENDING_ACTIVATION,               account_pending_activation",
        "INVALID_ACCOUNT_ACTIVATION_OTP,           invalid_account_activation_otp",
        "INVALID_PASSWORD_RESET_OTP,               invalid_password_reset_otp",
        "INVALID_PASSWORD_RESET_VERIFICATION_TOKEN,invalid_password_reset_verification_token",
        "CREDENTIAL_INTEGRITY_ERROR,               credential_integrity_error",
        "INVALID_TOKEN,                            invalid_token",
        "AUTHENTICATION_REQUIRED,                  authentication_required",
        "ACCESS_DENIED,                            access_denied",
        "RATE_LIMIT_EXCEEDED,                      rate_limit_exceeded",
        "ACCOUNT_TEMPORARILY_LOCKED,               account_temporarily_locked",
        "CAPTCHA_REQUIRED,                         captcha_required",
        "MALFORMED_REQUEST,                        malformed_request",
        "INFRASTRUCTURE_UNAVAILABLE,               infrastructure_unavailable",
        "DATA_INTEGRITY_ERROR,                     data_integrity_error",
        "INTERNAL_SERVER_ERROR,                    internal_server_error"
    })
    void getCode_everyConstant_mapsToExpectedCode(ErrorCode errorCode, String expectedCode) {

        assertThat(errorCode.getCode()).isEqualTo(expectedCode);
    }

    @Test
    void values_everyConstant_isCoveredByMappingTest() {

        assertThat(ErrorCode.values()).hasSize(21);
    }
}
