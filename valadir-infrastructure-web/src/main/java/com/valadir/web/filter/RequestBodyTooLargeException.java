package com.valadir.web.filter;

/**
 * The request body is larger than what may be buffered before the rate limiter has spoken.
 * Never leaves the filter package: {@link RateLimitFilter} answers it.
 */
class RequestBodyTooLargeException extends RuntimeException {

    RequestBodyTooLargeException(int maxBytes) {

        super("Request body exceeds the " + maxBytes + " byte buffering limit");
    }
}
