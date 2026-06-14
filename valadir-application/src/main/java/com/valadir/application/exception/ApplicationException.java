package com.valadir.application.exception;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

public class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;

    public static ApplicationException translate(DomainException e) {

        return new ApplicationException(e.getMessage(), e.getErrorCode(), e);
    }

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
