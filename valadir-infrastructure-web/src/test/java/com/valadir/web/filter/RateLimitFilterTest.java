package com.valadir.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.ratelimit.RateLimitResult;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.web.config.RateLimitProperties;
import com.valadir.web.config.RateLimitProperties.Strategy;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    private static final String EMAIL = "user@example.com";
    private static final int LIMIT = 10;
    private static final int WINDOW = 60;
    private static final String REDIS_KEY = "rate_limit:ip:api_auth_login:10.0.0.1";
    private static final String PATH_LOGIN = "/api/auth/login";

    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    private static final RateLimitProperties.Rule IP_RULE = new RateLimitProperties.Rule(PATH_LOGIN, Strategy.IP, LIMIT, WINDOW);
    private static final RateLimitProperties.Rule EMAIL_RULE = new RateLimitProperties.Rule(PATH_LOGIN, Strategy.EMAIL, 5, 900);

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private RateLimitKeyResolver keyResolver;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void doFilter_disabled_skipsRateLimitAndPassesThrough() throws Exception {

        final RateLimitFilter filter = buildFilter(false, List.of());
        final MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        then(rateLimiter).should(never()).consume(anyString(), anyInt(), anyInt());
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void doFilter_noMatchingRule_passesThrough() throws Exception {

        final RateLimitFilter filter = buildFilter(true, List.of(IP_RULE));
        final MockHttpServletRequest request = buildRequest("/api/other/endpoint");
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        then(rateLimiter).should(never()).consume(anyString(), anyInt(), anyInt());
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void doFilter_allowed_setsHeadersAndPassesThrough() throws Exception {

        given(keyResolver.resolve(any(HttpServletRequest.class), eq(IP_RULE))).willReturn(Optional.of(REDIS_KEY));
        given(rateLimiter.consume(REDIS_KEY, LIMIT, WINDOW)).willReturn(new RateLimitResult(true, 3L, LIMIT, 45L));

        final RateLimitFilter filter = buildFilter(true, List.of(IP_RULE));
        final MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getHeader(HEADER_LIMIT)).isEqualTo("10");
        assertThat(response.getHeader(HEADER_REMAINING)).isEqualTo("7");
        assertThat(response.getHeader(HEADER_RESET)).isNotNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void doFilter_blocked_returns429WithHeaders() throws Exception {

        given(keyResolver.resolve(any(HttpServletRequest.class), eq(IP_RULE))).willReturn(Optional.of(REDIS_KEY));
        given(rateLimiter.consume(REDIS_KEY, LIMIT, WINDOW)).willReturn(new RateLimitResult(false, 11L, LIMIT, 30L));

        final RateLimitFilter filter = buildFilter(true, List.of(IP_RULE));
        final MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getHeader(HEADER_LIMIT)).isEqualTo("10");
        assertThat(response.getHeader(HEADER_REMAINING)).isEqualTo("0");
        assertThat(response.getHeader(HEADER_RESET)).isNotNull();
        assertThat(response.getHeader(HEADER_RETRY_AFTER)).isEqualTo("30");
        assertThat(response.getContentAsString()).contains(ErrorCode.RATE_LIMIT_EXCEEDED.getCode());
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void doFilter_unresolvedKey_skipsRule() throws Exception {

        final var userRule = new RateLimitProperties.Rule(PATH_LOGIN, Strategy.USER, LIMIT, WINDOW);
        given(keyResolver.resolve(any(HttpServletRequest.class), eq(userRule))).willReturn(Optional.empty());

        final RateLimitFilter filter = buildFilter(true, List.of(userRule));
        final MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        then(rateLimiter).should(never()).consume(anyString(), anyInt(), anyInt());
        assertThat(response.getHeader(HEADER_LIMIT)).isNull();
        assertThat(response.getHeader(HEADER_REMAINING)).isNull();
        assertThat(response.getHeader(HEADER_RESET)).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void doFilter_emailStrategy_wrapsRequestSoBodyIsReadableByController() throws Exception {

        given(keyResolver.resolve(any(HttpServletRequest.class), eq(EMAIL_RULE))).willReturn(Optional.of(REDIS_KEY));
        given(rateLimiter.consume(REDIS_KEY, 5, 900)).willReturn(new RateLimitResult(true, 1L, 5, 900L));

        final RateLimitFilter filter = buildFilter(true, List.of(EMAIL_RULE));
        final MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        request.setContent(objectMapper.writeValueAsBytes(Map.of("email", EMAIL, "password", "secret")));
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(chain.getRequest()).isNotNull();
        // Body must still be readable by the controller (cached wrapper was passed down)
        final byte[] bodyAfterFilter = chain.getRequest().getInputStream().readAllBytes();
        assertThat(new String(bodyAfterFilter)).contains(EMAIL);
    }

    @Test
    void doFilter_multipleRules_headersReflectMostRestrictivePassingRule() throws Exception {

        given(keyResolver.resolve(any(HttpServletRequest.class), eq(IP_RULE))).willReturn(Optional.of("key:ip"));
        given(keyResolver.resolve(any(HttpServletRequest.class), eq(EMAIL_RULE))).willReturn(Optional.of("key:email"));
        given(rateLimiter.consume("key:ip", LIMIT, WINDOW)).willReturn(new RateLimitResult(true, 8L, LIMIT, 55L)); // 2 remaining
        given(rateLimiter.consume("key:email", 5, 900)).willReturn(new RateLimitResult(true, 2L, 5, 800L)); // 3 remaining

        final RateLimitFilter filter = buildFilter(true, List.of(IP_RULE, EMAIL_RULE));
        final MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // IP has 2 remaining, EMAIL has 3 remaining → IP is most restrictive
        assertThat(response.getHeader(HEADER_LIMIT)).isEqualTo("10");
        assertThat(response.getHeader(HEADER_REMAINING)).isEqualTo("2");
    }

    @Test
    void doFilter_multipleRulesSecondRuleBlocked_returns429WithoutAllowedHeadersFromFirstRule() throws Exception {

        given(keyResolver.resolve(any(HttpServletRequest.class), eq(IP_RULE))).willReturn(Optional.of("key:ip"));
        given(keyResolver.resolve(any(HttpServletRequest.class), eq(EMAIL_RULE))).willReturn(Optional.of("key:email"));
        given(rateLimiter.consume("key:ip", LIMIT, WINDOW)).willReturn(new RateLimitResult(true, 3L, LIMIT, 45L));
        given(rateLimiter.consume("key:email", 5, 900)).willReturn(new RateLimitResult(false, 6L, 5, 30L));

        final RateLimitFilter filter = buildFilter(true, List.of(IP_RULE, EMAIL_RULE));
        final MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        // IP's allowed headers must NOT leak into the 429 response
        assertThat(response.getHeader(HEADER_LIMIT)).isEqualTo("5");
        assertThat(response.getHeader(HEADER_REMAINING)).isEqualTo("0");
        assertThat(response.getHeader(HEADER_RETRY_AFTER)).isEqualTo("30");
        assertThat(response.getContentAsString()).contains(ErrorCode.RATE_LIMIT_EXCEEDED.getCode());
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void doFilter_multipleRules_failFastOnFirstBlockedRule() throws Exception {

        given(keyResolver.resolve(any(HttpServletRequest.class), eq(IP_RULE))).willReturn(Optional.of(REDIS_KEY));
        given(rateLimiter.consume(REDIS_KEY, LIMIT, WINDOW)).willReturn(new RateLimitResult(false, 11L, LIMIT, 30L));

        final RateLimitFilter filter = buildFilter(true, List.of(IP_RULE, EMAIL_RULE));
        final MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        final var response = new MockHttpServletResponse();
        final var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        // Only one consume was made (fail-fast after first blocked rule)
        then(rateLimiter).should().consume(REDIS_KEY, LIMIT, WINDOW);
        then(keyResolver).shouldHaveNoMoreInteractions();
        then(rateLimiter).shouldHaveNoMoreInteractions();
    }

    private RateLimitFilter buildFilter(final boolean enabled, final List<RateLimitProperties.Rule> rules) {

        return new RateLimitFilter(rateLimiter, new RateLimitProperties(enabled, rules), objectMapper, keyResolver);
    }

    private MockHttpServletRequest buildRequest(final String path) {

        final var request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}
