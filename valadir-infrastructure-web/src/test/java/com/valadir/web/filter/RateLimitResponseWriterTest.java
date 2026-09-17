package com.valadir.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.ratelimit.RateLimitResult;
import com.valadir.web.exception.HttpStatusResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitResponseWriterTest {

    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    private static final int MAX_REQUESTS = 10;
    private static final Duration REMAINING_TTL = Duration.ofSeconds(30);

    // Wide enough that a slow machine never fails, narrow enough that a wrong sign or unit does.
    private static final long CLOCK_TOLERANCE_SECONDS = 5;

    private final RateLimitResponseWriter writer = new RateLimitResponseWriter(new ObjectMapper(), new HttpStatusResolver());

    @Test
    void writeBlockedResponse_sets429StatusAndHeaders() throws Exception {

        var response = new MockHttpServletResponse();
        var result = new RateLimitResult(false, 11L, MAX_REQUESTS, REMAINING_TTL);

        writer.writeBlockedResponse(response, result);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getHeader(HEADER_LIMIT)).isEqualTo("10");
        assertThat(response.getHeader(HEADER_REMAINING)).isEqualTo("0");
        // The epoch second the window frees up. Asserting presence alone would not notice the
        // arithmetic drifting; this is the one place the number itself is pinned.
        long expectedReset = Instant.now().getEpochSecond() + REMAINING_TTL.toSeconds();
        assertThat(Long.parseLong(response.getHeader(HEADER_RESET)))
            .isBetween(expectedReset - CLOCK_TOLERANCE_SECONDS, expectedReset + CLOCK_TOLERANCE_SECONDS);
        assertThat(response.getHeader(HEADER_RETRY_AFTER)).isEqualTo("30");
    }

    @Test
    void writeBlockedResponse_bodyContainsRateLimitExceededCode() throws Exception {

        var response = new MockHttpServletResponse();
        var result = new RateLimitResult(false, 11L, MAX_REQUESTS, REMAINING_TTL);

        writer.writeBlockedResponse(response, result);

        assertThat(response.getContentAsString()).contains(ErrorCode.RATE_LIMIT_EXCEEDED.getCode());
    }

    @Test
    void writeAllowedRequestHeaders_setsRateLimitHeaders() {

        var response = new MockHttpServletResponse();
        var result = new RateLimitResult(true, 3L, MAX_REQUESTS, Duration.ofSeconds(45));

        writer.writeAllowedRequestHeaders(response, result);

        assertThat(response.getHeader(HEADER_LIMIT)).isEqualTo("10");
        assertThat(response.getHeader(HEADER_REMAINING)).isEqualTo("7");
        assertThat(response.getHeader(HEADER_RESET)).isNotNull();
    }

    @Test
    void writeAllowedRequestHeaders_remainingFlooredAtZero_whenRequestCountExceedsLimit() {

        var response = new MockHttpServletResponse();
        var result = new RateLimitResult(true, 15L, MAX_REQUESTS, Duration.ofSeconds(45));

        writer.writeAllowedRequestHeaders(response, result);

        assertThat(response.getHeader(HEADER_REMAINING)).isEqualTo("0");
    }
}
