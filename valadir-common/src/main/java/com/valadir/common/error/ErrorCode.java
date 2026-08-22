package com.valadir.common.error;

public enum ErrorCode {

    // Validation (VAL-xxx)
    INVALID_FIELD("VAL-001", Category.VALIDATION),
    INVALID_PASSWORD("VAL-002", Category.VALIDATION),
    REQUIRED_FIELD_MISSING("VAL-003", Category.VALIDATION),
    INVALID_OTP("VAL-004", Category.VALIDATION),

    // Business Rules (BIZ-xxx)
    INSECURE_PASSWORD("BIZ-001", Category.VALIDATION),
    EMAIL_ALREADY_EXISTS("BIZ-002", Category.CONFLICT),
    AUTHENTICATION_FAILED("BIZ-003", Category.UNAUTHORIZED),
    ACCOUNT_PENDING_ACTIVATION("BIZ-004", Category.FORBIDDEN),
    INVALID_ACCOUNT_ACTIVATION_OTP("BIZ-005", Category.UNAUTHORIZED),
    INVALID_PASSWORD_RESET_OTP("BIZ-006", Category.UNAUTHORIZED),
    INVALID_PASSWORD_RESET_VERIFICATION_TOKEN("BIZ-007", Category.UNAUTHORIZED),

    // Security (SEC-xxx)
    CREDENTIAL_INTEGRITY_ERROR("SEC-001", Category.UNAUTHORIZED),
    INVALID_TOKEN("SEC-002", Category.UNAUTHORIZED),
    AUTHENTICATION_REQUIRED("SEC-003", Category.UNAUTHORIZED),
    ACCESS_DENIED("SEC-004", Category.FORBIDDEN),
    RATE_LIMIT_EXCEEDED("SEC-005", Category.RATE_LIMITED),
    ACCOUNT_TEMPORARILY_LOCKED("SEC-006", Category.RATE_LIMITED),
    CAPTCHA_REQUIRED("SEC-007", Category.FORBIDDEN),

    // Malformed request (REQ-xxx) — the request never reached a use case; VAL-xxx means a field was
    // validated and failed. One code for all of them: the status already tells 400 from 404, 405 and 415.
    MALFORMED_REQUEST("REQ-001", Category.VALIDATION),

    // Infrastructure (INFRA-xxx)
    INFRASTRUCTURE_UNAVAILABLE("INFRA-001", Category.SERVER_ERROR),

    // Persistence (PER-xxx)
    DATA_INTEGRITY_ERROR("PER-001", Category.SERVER_ERROR),

    // System (SYS-xxx)
    INTERNAL_SERVER_ERROR("SYS-001", Category.SERVER_ERROR);

    public enum Category {
        VALIDATION,
        CONFLICT,
        UNAUTHORIZED,
        FORBIDDEN,
        RATE_LIMITED,
        SERVER_ERROR
    }

    private final String code;
    private final Category category;

    ErrorCode(String code, Category category) {

        this.code = code;
        this.category = category;
    }

    public String getCode() {

        return code;
    }

    public Category getCategory() {

        return category;
    }
}
