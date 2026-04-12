package com.valadir.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.ratelimit.RateLimitResult;
import com.valadir.web.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

public class RateLimitResponseWriter {

    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    private final ObjectMapper objectMapper;

    public RateLimitResponseWriter(final ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    public void writeBlockedResponse(final HttpServletResponse response, final RateLimitResult result) throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HEADER_LIMIT, String.valueOf(result.maxRequests()));
        response.setHeader(HEADER_REMAINING, "0");
        response.setHeader(HEADER_RESET, String.valueOf(resetEpochSeconds(result)));
        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(result.remainingTtl()));
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(ErrorCode.RATE_LIMIT_EXCEEDED.getCode()));
    }

    public void writeAllowedRequestHeaders(final HttpServletResponse response, final RateLimitResult result) {

        response.setHeader(HEADER_LIMIT, String.valueOf(result.maxRequests()));
        response.setHeader(HEADER_REMAINING, String.valueOf(result.remaining()));
        response.setHeader(HEADER_RESET, String.valueOf(resetEpochSeconds(result)));
    }

    private long resetEpochSeconds(final RateLimitResult result) {

        return System.currentTimeMillis() / 1000L + result.remainingTtl();
    }
}
