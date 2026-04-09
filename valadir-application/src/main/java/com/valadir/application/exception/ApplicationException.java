package com.valadir.application.exception;

import com.valadir.common.error.ErrorCode;

public class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApplicationException(String message, ErrorCode errorCode) {

        super(message);
        this.errorCode = errorCode;
    }

    public ApplicationException(String message, ErrorCode errorCode, Throwable cause) {

        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {

        return errorCode;
    }
}
