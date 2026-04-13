package com.valadir.common.exception;

import com.valadir.common.error.ErrorCode;

public class InfrastructureException extends RuntimeException {

    private final ErrorCode errorCode;

    public InfrastructureException(String message, Throwable cause) {

        super(message, cause);
        this.errorCode = ErrorCode.INFRASTRUCTURE_UNAVAILABLE;
    }

    public ErrorCode getErrorCode() {

        return errorCode;
    }
}
