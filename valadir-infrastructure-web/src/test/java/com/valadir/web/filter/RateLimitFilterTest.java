package com.valadir.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.common.ratelimit.RateLimitResult;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.web.config.RateLimitProperties;
import com.valadir.web.config.RateLimitProperties.Strategy;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private static final int MAX_REQUESTS = 10;
    private static final int WINDOW = 60;
    private static final String IP_REDIS_KEY = "rate_limit:ip:api_auth_login:10.0.0.1";
    private static final String EMAIL_REDIS_KEY = "rate_limit:email:api_auth_login:user@example.com";
    private static final String PATH_LOGIN = "/api/auth/login";

    private static final RateLimitProperties.Rule IP_RULE = new RateLimitProperties.Rule(PATH_LOGIN, Strategy.IP, MAX_REQUESTS, WINDOW);
    private static final RateLimitProperties.Rule EMAIL_RULE = new RateLimitProperties.Rule(PATH_LOGIN, Strategy.EMAIL, 5, 900);

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private RateLimitKeyResolver keyResolver;

    @Mock
    private RateLimitResponseWriter responseWriter;

    @Captor
    private ArgumentCaptor<RateLimitResult> resultCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void doFilter_disabled_skipsRateLimitAndPassesThrough() throws Exception {

        RateLimitFilter filter = buildFilter(false, List.of());
        MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        then(rateLimiter).should(never()).consume(anyString(), anyInt(), anyInt());
        then(responseWriter).shouldHaveNoInteractions();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void doFilter_noMatchingRule_passesThrough() throws Exception {

        RateLimitFilter filter = buildFilter(true, List.of(IP_RULE));
        MockHttpServletRequest request = buildRequest("/api/other/endpoint");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        then(rateLimiter).should(never()).consume(anyString(), anyInt(), anyInt());
        then(responseWriter).shouldHaveNoInteractions();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void doFilter_allowed_delegatesToWriteAllowedHeadersAndPassesThrough() throws Exception {

        var allowedResult = new RateLimitResult(true, 3L, MAX_REQUESTS, 45L);
        given(keyResolver.resolve(any(HttpServletRequest.class), eq(IP_RULE))).willReturn(Optional.of(IP_REDIS_KEY));
        given(rateLimiter.consume(IP_REDIS_KEY, MAX_REQUESTS, WINDOW)).willReturn(allowedResult);

        RateLimitFilter filter = buildFilter(true, List.of(IP_RULE));
        MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        then(responseWriter).should().writeAllowedRequestHeaders(response, allowedResult);
        then(responseWriter).should(never()).writeBlockedResponse(any(), any());
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void doFilter_blocked_delegatesToWriteBlockedResponseAndStopsChain() throws Exception {

        var blockedResult = new RateLimitResult(false, 11L, MAX_REQUESTS, 30L);
        given(keyResolver.resolve(any(HttpServletRequest.class), eq(IP_RULE))).willReturn(Optional.of(IP_REDIS_KEY));
        given(rateLimiter.consume(IP_REDIS_KEY, MAX_REQUESTS, WINDOW)).willReturn(blockedResult);

        RateLimitFilter filter = buildFilter(true, List.of(IP_RULE));
        MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        then(responseWriter).should().writeBlockedResponse(response, blockedResult);
        then(responseWriter).should(never()).writeAllowedRequestHeaders(any(), any());
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void doFilter_unresolvedKey_skipsRuleAndDoesNotWriteHeaders() throws Exception {

        var userRule = new RateLimitProperties.Rule(PATH_LOGIN, Strategy.USER, MAX_REQUESTS, WINDOW);
        given(keyResolver.resolve(any(HttpServletRequest.class), eq(userRule))).willReturn(Optional.empty());

        RateLimitFilter filter = buildFilter(true, List.of(userRule));
        MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        then(rateLimiter).should(never()).consume(anyString(), anyInt(), anyInt());
        then(responseWriter).shouldHaveNoInteractions();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void doFilter_emailStrategy_wrapsRequestSoBodyIsReadableByController() throws Exception {

        given(keyResolver.resolve(any(HttpServletRequest.class), eq(EMAIL_RULE))).willReturn(Optional.of(IP_REDIS_KEY));
        given(rateLimiter.consume(IP_REDIS_KEY, 5, 900)).willReturn(new RateLimitResult(true, 1L, 5, 900L));

        RateLimitFilter filter = buildFilter(true, List.of(EMAIL_RULE));
        MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        request.setContent(objectMapper.writeValueAsBytes(Map.of("email", EMAIL, "password", "secret")));
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        // Body must still be readable by the controller (cached wrapper was passed down)
        byte[] bodyAfterFilter = chain.getRequest().getInputStream().readAllBytes();
        assertThat(new String(bodyAfterFilter)).contains(EMAIL);
    }

    @Test
    void doFilter_multipleRules_passesTheMostRestrictiveResultToWriteAllowedHeaders() throws Exception {

        var ipResult = new RateLimitResult(true, 8L, MAX_REQUESTS, 55L);    // 2 remaining
        var emailResult = new RateLimitResult(true, 2L, 5, 800L);    // 3 remaining
        given(keyResolver.resolve(any(HttpServletRequest.class), eq(IP_RULE))).willReturn(Optional.of(IP_REDIS_KEY));
        given(keyResolver.resolve(any(HttpServletRequest.class), eq(EMAIL_RULE))).willReturn(Optional.of(EMAIL_REDIS_KEY));
        given(rateLimiter.consume(IP_REDIS_KEY, MAX_REQUESTS, WINDOW)).willReturn(ipResult);
        given(rateLimiter.consume(EMAIL_REDIS_KEY, 5, 900)).willReturn(emailResult);

        RateLimitFilter filter = buildFilter(true, List.of(IP_RULE, EMAIL_RULE));
        MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // IP has 2 remaining, EMAIL has 3 remaining → IP is most restrictive
        then(responseWriter).should().writeAllowedRequestHeaders(eq(response), resultCaptor.capture());
        assertThat(resultCaptor.getValue()).isEqualTo(ipResult);
    }

    @Test
    void doFilter_multipleRulesSecondRuleBlocked_writesBlockedResponseForBlockedRuleOnly() throws Exception {

        var allowedResult = new RateLimitResult(true, 3L, MAX_REQUESTS, 45L);
        var blockedResult = new RateLimitResult(false, 6L, 5, 30L);
        given(keyResolver.resolve(any(HttpServletRequest.class), eq(IP_RULE))).willReturn(Optional.of(IP_REDIS_KEY));
        given(keyResolver.resolve(any(HttpServletRequest.class), eq(EMAIL_RULE))).willReturn(Optional.of(EMAIL_REDIS_KEY));
        given(rateLimiter.consume(IP_REDIS_KEY, MAX_REQUESTS, WINDOW)).willReturn(allowedResult);
        given(rateLimiter.consume(EMAIL_REDIS_KEY, 5, 900)).willReturn(blockedResult);

        RateLimitFilter filter = buildFilter(true, List.of(IP_RULE, EMAIL_RULE));
        MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // IP's result must NOT reach writeAllowedRequestHeaders — only the blocked EMAIL result reaches writeBlockedResponse
        then(responseWriter).should().writeBlockedResponse(response, blockedResult);
        then(responseWriter).should(never()).writeAllowedRequestHeaders(any(), any());
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void doFilter_multipleRules_failFastOnFirstBlockedRule() throws Exception {

        var blockedResult = new RateLimitResult(false, 11L, MAX_REQUESTS, 30L);
        given(keyResolver.resolve(any(HttpServletRequest.class), eq(IP_RULE))).willReturn(Optional.of(IP_REDIS_KEY));
        given(rateLimiter.consume(IP_REDIS_KEY, MAX_REQUESTS, WINDOW)).willReturn(blockedResult);

        RateLimitFilter filter = buildFilter(true, List.of(IP_RULE, EMAIL_RULE));
        MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // Only one consume was made (fail-fast after first blocked rule)
        then(keyResolver).shouldHaveNoMoreInteractions();
        then(rateLimiter).shouldHaveNoMoreInteractions();
        then(responseWriter).should().writeBlockedResponse(response, blockedResult);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void doFilter_rateLimiterUnavailable_failsOpen() throws Exception {

        given(keyResolver.resolve(any(HttpServletRequest.class), eq(IP_RULE))).willReturn(Optional.of(IP_REDIS_KEY));
        given(rateLimiter.consume(IP_REDIS_KEY, MAX_REQUESTS, WINDOW)).willThrow(new InfrastructureException("Redis unavailable — rate limit check failed", new RuntimeException()));

        RateLimitFilter filter = buildFilter(true, List.of(IP_RULE));
        MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        then(responseWriter).shouldHaveNoInteractions();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void doFilter_rateLimiterUnavailableOnFirstRule_evaluatesRemainingRules() throws Exception {

        var emailResult = new RateLimitResult(true, 1L, 5, 900L);
        given(keyResolver.resolve(any(HttpServletRequest.class), eq(IP_RULE))).willReturn(Optional.of(IP_REDIS_KEY));
        given(keyResolver.resolve(any(HttpServletRequest.class), eq(EMAIL_RULE))).willReturn(Optional.of(EMAIL_REDIS_KEY));
        given(rateLimiter.consume(IP_REDIS_KEY, MAX_REQUESTS, WINDOW)).willThrow(new InfrastructureException("Redis unavailable — rate limit check failed", new RuntimeException()));
        given(rateLimiter.consume(EMAIL_REDIS_KEY, 5, 900)).willReturn(emailResult);

        RateLimitFilter filter = buildFilter(true, List.of(IP_RULE, EMAIL_RULE));
        MockHttpServletRequest request = buildRequest(PATH_LOGIN);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        then(responseWriter).should().writeAllowedRequestHeaders(response, emailResult);
        then(responseWriter).shouldHaveNoMoreInteractions();
        assertThat(chain.getRequest()).isNotNull();
    }

    private RateLimitFilter buildFilter(boolean enabled, List<RateLimitProperties.Rule> rules) {

        return new RateLimitFilter(rateLimiter, new RateLimitProperties(enabled, rules), responseWriter, keyResolver);
    }

    private MockHttpServletRequest buildRequest(String path) {

        var request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}
