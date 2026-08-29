package com.valadir.common.error;

public enum ErrorCode {

    // Validation (VAL-xxx)
    INVALID_FIELD("VAL-001"),
    INVALID_PASSWORD("VAL-002"),
    REQUIRED_FIELD_MISSING("VAL-003"),
    INVALID_OTP("VAL-004"),

    // Business Rules (BIZ-xxx)
    INSECURE_PASSWORD("BIZ-001"),
    EMAIL_ALREADY_EXISTS("BIZ-002"),
    ACCOUNT_PENDING_ACTIVATION("BIZ-003"),
    INVALID_ACCOUNT_ACTIVATION_OTP("BIZ-004"),
    INVALID_PASSWORD_RESET_OTP("BIZ-005"),
    INVALID_PASSWORD_RESET_VERIFICATION_TOKEN("BIZ-006"),

    // Security (SEC-xxx)
    CREDENTIAL_INTEGRITY_ERROR("SEC-001"),
    INVALID_TOKEN("SEC-002"),
    AUTHENTICATION_REQUIRED("SEC-003"),
    ACCESS_DENIED("SEC-004"),
    RATE_LIMIT_EXCEEDED("SEC-005"),
    ACCOUNT_TEMPORARILY_LOCKED("SEC-006"),
    CAPTCHA_REQUIRED("SEC-007"),

    // Malformed request (REQ-xxx) — the request never reached a use case; VAL-xxx means a field was
    // validated and failed. One code for all of them: the status already tells 400 from 404, 405 and 415.
    MALFORMED_REQUEST("REQ-001"),

    // Infrastructure (INFRA-xxx)
    INFRASTRUCTURE_UNAVAILABLE("INFRA-001"),

    // Persistence (PER-xxx)
    DATA_INTEGRITY_ERROR("PER-001"),

    // System (SYS-xxx)
    INTERNAL_SERVER_ERROR("SYS-001");

    private final String code;

    ErrorCode(String code) {

        this.code = code;
    }

    public String getCode() {

        return code;
    }
}
