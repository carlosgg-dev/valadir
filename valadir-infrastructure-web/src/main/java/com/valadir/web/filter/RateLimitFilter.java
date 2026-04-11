package com.valadir.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.common.ratelimit.RateLimitResult;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.web.config.RateLimitProperties;
import com.valadir.web.config.RateLimitProperties.Strategy;
import com.valadir.web.dto.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final RateLimitKeyResolver keyResolver;
    private final AntPathMatcher pathMatcher;

    public RateLimitFilter(
        final RateLimiter rateLimiter,
        final RateLimitProperties properties,
        final ObjectMapper objectMapper,
        final RateLimitKeyResolver keyResolver
    ) {

        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.keyResolver = keyResolver;
        this.pathMatcher = new AntPathMatcher();
    }

    @Override
    protected void doFilterInternal(
        @NonNull final HttpServletRequest request,
        @NonNull final HttpServletResponse response,
        @NonNull final FilterChain chain
    ) throws ServletException, IOException {

        final List<RateLimitProperties.Rule> matchingRules = matchingRules(request.getRequestURI());
        if (!properties.enabled() || matchingRules.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        final HttpServletRequest effectiveRequest = prepareRequest(request, matchingRules);

        if (isRateLimited(effectiveRequest, response, matchingRules)) {
            return;
        }

        chain.doFilter(effectiveRequest, response);
    }

    private List<RateLimitProperties.Rule> matchingRules(final String path) {

        return properties.rules().stream()
            .filter(rule -> pathMatcher.match(rule.path(), path))
            .toList();
    }

    private HttpServletRequest prepareRequest(
        final HttpServletRequest request,
        final List<RateLimitProperties.Rule> rules
    ) throws IOException {

        final boolean needsBodyRead = rules.stream().anyMatch(rule -> rule.strategy() == Strategy.EMAIL);

        return needsBodyRead
            ? new CachedBodyRequestWrapper(request)
            : request;
    }

    private boolean isRateLimited(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final List<RateLimitProperties.Rule> rules
    ) throws IOException {

        // Most constrained result across rules — used only to populate response headers
        // This does not affect the block/pass decision
        RateLimitResult mostRestrictive = null;

        for (final RateLimitProperties.Rule rule : rules) {
            final Optional<String> redisKey = keyResolver.resolve(request, rule);
            if (redisKey.isPresent()) {
                final RateLimitResult result;
                try {
                    result = rateLimiter.consume(redisKey.get(), rule.maxRequests(), rule.windowSeconds());
                } catch (InfrastructureException e) {
                    log.warn("Rate limiter unavailable, failing open: {}", e.getMessage());
                    continue;
                }

                if (!result.allowed()) {
                    log.warn("Rate limit exceeded: strategy={} key={}", rule.strategy(), redisKey.get());
                    writeRateLimitResponse(response, result);
                    return true;
                }

                if (mostRestrictive == null || remaining(result) < remaining(mostRestrictive)) {
                    mostRestrictive = result;
                }
            }
        }

        // headers are only set when at least one rule was evaluated and allowed
        if (mostRestrictive != null) {
            setRateLimitHeaders(response, mostRestrictive);
        }

        return false;
    }

    private void setRateLimitHeaders(final HttpServletResponse response, final RateLimitResult result) {

        response.setHeader(HEADER_LIMIT, String.valueOf(result.maxRequests()));
        response.setHeader(HEADER_REMAINING, String.valueOf(remaining(result)));
        response.setHeader(HEADER_RESET, String.valueOf(resetEpochSeconds(result)));
    }

    private void writeRateLimitResponse(final HttpServletResponse response, final RateLimitResult result) throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HEADER_LIMIT, String.valueOf(result.maxRequests()));
        response.setHeader(HEADER_REMAINING, "0");
        response.setHeader(HEADER_RESET, String.valueOf(resetEpochSeconds(result)));
        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(result.remainingTtl()));
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(ErrorCode.RATE_LIMIT_EXCEEDED.getCode()));
    }

    private long remaining(final RateLimitResult result) {

        return Math.max(0L, result.maxRequests() - result.requestCount());
    }

    private long resetEpochSeconds(final RateLimitResult result) {

        return System.currentTimeMillis() / 1000L + result.remainingTtl();
    }
}
