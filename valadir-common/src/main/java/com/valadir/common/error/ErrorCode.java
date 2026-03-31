package com.valadir.common.error;

public enum ErrorCode {

    // Validation (VAL-xxx)
    INVALID_FIELD("VAL-001"),
    INVALID_PASSWORD("VAL-002"),
    REQUIRED_FIELD_MISSING("VAL-003"),

    // Business Rules (BIZ-xxx)
    INSECURE_PASSWORD("BIZ-001"),
    EMAIL_ALREADY_EXISTS("BIZ-002"),
    CREDENTIAL_INTEGRITY_ERROR("BIZ-003"),

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
