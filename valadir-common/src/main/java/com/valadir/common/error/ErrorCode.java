package com.valadir.common.error;

public enum ErrorCode {

    // Validation (VAL-xxx)
    INVALID_FIELD("VAL-001"),
    INVALID_PASSWORD("VAL-002"),
    REQUIRED_FIELD_MISSING("VAL-003"),

    // Business Rules (BIZ-xxx)
    INSECURE_PASSWORD("BIZ-001"),
    EMAIL_ALREADY_EXISTS("BIZ-002"),

    // Security (SEC-xxx)
    CREDENTIAL_INTEGRITY_ERROR("SEC-001"),
    TOKEN_REUSE_DETECTED("SEC-002"),
    TOKEN_REVOCATION_FAILED("SEC-003"),
    INVALID_TOKEN("SEC-004"),

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
