package com.valadir.common.error;

public enum ErrorCode {

    // Validation (VAL-xxx)
    INVALID_FIELD("VAL-001", Category.VALIDATION),
    INVALID_PASSWORD("VAL-002", Category.VALIDATION),
    REQUIRED_FIELD_MISSING("VAL-003", Category.VALIDATION),

    // Business Rules (BIZ-xxx)
    INSECURE_PASSWORD("BIZ-001", Category.VALIDATION),
    EMAIL_ALREADY_EXISTS("BIZ-002", Category.CONFLICT),
    AUTHENTICATION_FAILED("BIZ-003", Category.UNAUTHORIZED),

    // Security (SEC-xxx)
    CREDENTIAL_INTEGRITY_ERROR("SEC-001", Category.UNAUTHORIZED),
    TOKEN_REVOCATION_FAILED("SEC-002", Category.SERVER_ERROR),
    INVALID_TOKEN("SEC-003", Category.UNAUTHORIZED),
    AUTHENTICATION_REQUIRED("SEC-004", Category.UNAUTHORIZED),
    ACCESS_DENIED("SEC-005", Category.FORBIDDEN),
    RATE_LIMIT_EXCEEDED("SEC-006", Category.RATE_LIMITED),

    // Infrastructure (INFRA-xxx)
    INFRASTRUCTURE_UNAVAILABLE("INFRA-001", Category.SERVER_ERROR),

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

    ErrorCode(final String code, final Category category) {

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
