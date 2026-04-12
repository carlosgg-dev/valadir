package com.valadir.web.exception;

import com.valadir.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class HttpStatusResolver {

    HttpStatus resolve(final ErrorCode code) {

        return switch (code.getCategory()) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
