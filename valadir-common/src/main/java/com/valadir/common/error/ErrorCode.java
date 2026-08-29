package com.valadir.common.error;

import java.util.Locale;

public enum ErrorCode {

    // Validation
    INVALID_FIELD,
    INVALID_PASSWORD,
    REQUIRED_FIELD_MISSING,
    INVALID_OTP,

    // Business rules
    INSECURE_PASSWORD,
    EMAIL_ALREADY_EXISTS,
    ACCOUNT_PENDING_ACTIVATION,
    INVALID_ACCOUNT_ACTIVATION_OTP,
    INVALID_PASSWORD_RESET_OTP,
    INVALID_PASSWORD_RESET_VERIFICATION_TOKEN,

    // Security
    CREDENTIAL_INTEGRITY_ERROR,
    INVALID_TOKEN,
    AUTHENTICATION_REQUIRED,
    ACCESS_DENIED,
    RATE_LIMIT_EXCEEDED,
    ACCOUNT_TEMPORARILY_LOCKED,
    CAPTCHA_REQUIRED,

    // Malformed request — the request never reached a use case, unlike the validation codes above,
    // which report a field that was validated and failed. One code for all of them: the status
    // already tells 400 from 404, 405 and 415.
    MALFORMED_REQUEST,

    // Infrastructure
    INFRASTRUCTURE_UNAVAILABLE,

    // Persistence
    DATA_INTEGRITY_ERROR,

    // System
    INTERNAL_SERVER_ERROR;

    // Locale.ROOT is not decoration: the default-locale overload maps I to ı under tr-TR,
    // which would make the published identifier depend on where the JVM happens to start
    public String getCode() {

        return name().toLowerCase(Locale.ROOT);
    }
}
