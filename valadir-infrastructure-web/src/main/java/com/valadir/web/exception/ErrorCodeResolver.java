package com.valadir.web.exception;

import com.valadir.common.error.ErrorCode;
import org.springframework.http.HttpStatusCode;

/**
 * The inverse of {@link HttpStatusResolver}: where the application throws, the code decides the
 * status; where the framework rejects, the status is given and the code is what must be derived.
 */
public class ErrorCodeResolver {

    public ErrorCode resolve(HttpStatusCode status) {

        return status.is4xxClientError()
            ? ErrorCode.MALFORMED_REQUEST
            : ErrorCode.INTERNAL_SERVER_ERROR;
    }
}
