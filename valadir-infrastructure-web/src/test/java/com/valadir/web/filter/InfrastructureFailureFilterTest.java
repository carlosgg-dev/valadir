package com.valadir.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.web.dto.response.ErrorResponse;
import com.valadir.web.exception.HttpStatusResolver;
import com.valadir.web.exception.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.UnsupportedEncodingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class InfrastructureFailureFilterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final InfrastructureFailureFilter filter =
        new InfrastructureFailureFilter(new SecurityErrorResponseWriter(OBJECT_MAPPER, new HttpStatusResolver()));
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void doFilter_dependencyUnavailable_answers503WithTheInfrastructureErrorCode() throws Exception {

        FilterChain failingChain = (req, res) -> {
            throw new InfrastructureException("Redis unavailable — blacklist read failed for jti: abc");
        };

        filter.doFilter(request, response, failingChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = OBJECT_MAPPER.readValue(response.getContentAsString(), ErrorResponse.class);
        assertThat(body.code()).isEqualTo(ErrorCode.INFRASTRUCTURE_UNAVAILABLE.getCode());
        assertThat(body.errors()).isNull();
    }

    @Test
    void doFilter_dependencyUnavailable_leaksNothingAboutTheFailure() throws Exception {

        String internalDetail = "Redis unavailable at redis://cache-01:6379 — key auth:blacklist:abc";
        FilterChain failingChain = (req, res) -> {
            throw new InfrastructureException(internalDetail);
        };

        filter.doFilter(request, response, failingChain);

        assertThat(response.getContentAsString()).doesNotContain("redis", "cache-01", "6379", "auth:blacklist");
    }

    @Test
    void doFilter_unrelatedFailure_propagatesInsteadOfBecomingAnOutage() throws UnsupportedEncodingException {

        var bug = new IllegalStateException("a genuine defect");
        FilterChain failingChain = (req, res) -> {
            throw bug;
        };

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> filter.doFilter(request, response, failingChain))
            .isSameAs(bug);

        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void doFilter_noFailure_leavesTheResponseToTheChain() throws Exception {

        FilterChain succeedingChain = (req, res) -> response.setStatus(HttpStatus.OK.value());

        filter.doFilter(request, response, succeedingChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEmpty();
    }
}
