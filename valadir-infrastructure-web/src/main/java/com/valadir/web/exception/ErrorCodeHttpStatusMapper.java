package com.valadir.web.exception;

import com.valadir.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.Map;

class ErrorCodeHttpStatusMapper {

    private static final Map<ErrorCode, HttpStatus> MAPPINGS = Map.of(
        ErrorCode.EMAIL_ALREADY_EXISTS, HttpStatus.CONFLICT,
        ErrorCode.ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND,
        ErrorCode.CREDENTIAL_INTEGRITY_ERROR, HttpStatus.UNAUTHORIZED,
        ErrorCode.INVALID_TOKEN, HttpStatus.UNAUTHORIZED,
        ErrorCode.TOKEN_REVOCATION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR
    );

    private ErrorCodeHttpStatusMapper() {

    }

    static HttpStatus resolve(final ErrorCode code) {

        return MAPPINGS.getOrDefault(code, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
