package com.valadir.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.ratelimit.RateLimitResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitResponseWriterTest {

    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    private static final int MAX_REQUESTS = 10;
    private static final long REMAINING_TTL = 30L;

    private final RateLimitResponseWriter writer = new RateLimitResponseWriter(new ObjectMapper());

    @Test
    void writeBlockedResponse_sets429StatusAndHeaders() throws Exception {

        final var response = new MockHttpServletResponse();
        final var result = new RateLimitResult(false, 11L, MAX_REQUESTS, REMAINING_TTL);

        writer.writeBlockedResponse(response, result);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getHeader(HEADER_LIMIT)).isEqualTo("10");
        assertThat(response.getHeader(HEADER_REMAINING)).isEqualTo("0");
        assertThat(response.getHeader(HEADER_RESET)).isNotNull();
        assertThat(response.getHeader(HEADER_RETRY_AFTER)).isEqualTo("30");
    }

    @Test
    void writeBlockedResponse_bodyContainsRateLimitExceededCode() throws Exception {

        final var response = new MockHttpServletResponse();
        final var result = new RateLimitResult(false, 11L, MAX_REQUESTS, REMAINING_TTL);

        writer.writeBlockedResponse(response, result);

        assertThat(response.getContentAsString()).contains(ErrorCode.RATE_LIMIT_EXCEEDED.getCode());
    }

    @Test
    void writeAllowedRequestHeaders_setsRateLimitHeaders() {

        final var response = new MockHttpServletResponse();
        final var result = new RateLimitResult(true, 3L, MAX_REQUESTS, 45L);

        writer.writeAllowedRequestHeaders(response, result);

        assertThat(response.getHeader(HEADER_LIMIT)).isEqualTo("10");
        assertThat(response.getHeader(HEADER_REMAINING)).isEqualTo("7");
        assertThat(response.getHeader(HEADER_RESET)).isNotNull();
    }

    @Test
    void writeAllowedRequestHeaders_remainingFlooredAtZero_whenRequestCountExceedsLimit() {

        final var response = new MockHttpServletResponse();
        final var result = new RateLimitResult(true, 15L, MAX_REQUESTS, 45L);

        writer.writeAllowedRequestHeaders(response, result);

        assertThat(response.getHeader(HEADER_REMAINING)).isEqualTo("0");
    }
}
