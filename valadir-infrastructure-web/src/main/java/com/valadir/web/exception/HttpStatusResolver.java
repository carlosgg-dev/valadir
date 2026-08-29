package com.valadir.web.exception;

import com.valadir.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class HttpStatusResolver {

    public HttpStatus resolve(ErrorCode code) {

        return switch (code) {
            case INVALID_FIELD,
                 INVALID_PASSWORD,
                 REQUIRED_FIELD_MISSING,
                 INVALID_OTP,
                 INSECURE_PASSWORD,
                 MALFORMED_REQUEST -> HttpStatus.BAD_REQUEST;

            case EMAIL_ALREADY_EXISTS -> HttpStatus.CONFLICT;

            case INVALID_ACCOUNT_ACTIVATION_OTP,
                 INVALID_PASSWORD_RESET_OTP,
                 INVALID_PASSWORD_RESET_VERIFICATION_TOKEN,
                 CREDENTIAL_INTEGRITY_ERROR,
                 INVALID_TOKEN,
                 AUTHENTICATION_REQUIRED -> HttpStatus.UNAUTHORIZED;

            case ACCOUNT_PENDING_ACTIVATION,
                 ACCESS_DENIED,
                 CAPTCHA_REQUIRED -> HttpStatus.FORBIDDEN;

            case RATE_LIMIT_EXCEEDED,
                 ACCOUNT_TEMPORARILY_LOCKED -> HttpStatus.TOO_MANY_REQUESTS;

            case INFRASTRUCTURE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;

            case DATA_INTEGRITY_ERROR,
                 INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
