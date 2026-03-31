package com.valadir.domain.exception;

import com.valadir.common.error.ErrorCode;

public class DomainException extends RuntimeException {

    private final ErrorCode errorCode;

    public DomainException(String message, ErrorCode errorCode) {

        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {

        return errorCode;
    }
}
