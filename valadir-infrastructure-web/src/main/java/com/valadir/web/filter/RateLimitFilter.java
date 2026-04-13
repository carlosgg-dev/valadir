package com.valadir.web.filter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.common.ratelimit.RateLimitResult;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.web.config.RateLimitProperties;
import com.valadir.web.config.RateLimitProperties.Strategy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final RateLimitResponseWriter responseWriter;
    private final RateLimitKeyResolver keyResolver;
    private final AntPathMatcher pathMatcher;

    public RateLimitFilter(
        RateLimiter rateLimiter,
        RateLimitProperties properties,
        RateLimitResponseWriter responseWriter,
        RateLimitKeyResolver keyResolver
    ) {

        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.responseWriter = responseWriter;
        this.keyResolver = keyResolver;
        this.pathMatcher = new AntPathMatcher();
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain
    ) throws ServletException, IOException {

        List<RateLimitProperties.Rule> matchingRules = matchingRules(request.getRequestURI());
        if (!properties.enabled() || matchingRules.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest effectiveRequest = prepareRequest(request, matchingRules);

        if (isRateLimited(effectiveRequest, response, matchingRules)) {
            return;
        }

        chain.doFilter(effectiveRequest, response);
    }

    private List<RateLimitProperties.Rule> matchingRules(String path) {

        return properties.rules().stream()
            .filter(rule -> pathMatcher.match(rule.path(), path))
            .toList();
    }

    private HttpServletRequest prepareRequest(HttpServletRequest request, List<RateLimitProperties.Rule> rules) throws IOException {

        boolean needsBodyRead = rules.stream().anyMatch(rule -> rule.strategy() == Strategy.EMAIL);

        return needsBodyRead
            ? new CachedBodyRequestWrapper(request)
            : request;
    }

    private boolean isRateLimited(HttpServletRequest request, HttpServletResponse response, List<RateLimitProperties.Rule> rules) throws IOException {

        // Most constrained result across rules — used only to populate response headers
        // This does not affect the block/pass decision
        RateLimitResult mostRestrictive = null;

        for (RateLimitProperties.Rule rule : rules) {
            Optional<String> redisKey = keyResolver.resolve(request, rule);
            if (redisKey.isPresent()) {
                RateLimitResult result;
                try {
                    result = rateLimiter.consume(redisKey.get(), rule.maxRequests(), rule.windowSeconds());
                } catch (InfrastructureException e) {
                    log.warn("Rate limiter unavailable, failing open: {}", e.getMessage());
                    continue;
                }

                if (!result.allowed()) {
                    log.warn("Rate limit exceeded: strategy={} key={}", rule.strategy(), redisKey.get());
                    responseWriter.writeBlockedResponse(response, result);
                    return true;
                }

                if (mostRestrictive == null || result.isMoreRestrictiveThan(mostRestrictive)) {
                    mostRestrictive = result;
                }
            }
        }

        // headers are only set when at least one rule was evaluated and allowed
        if (mostRestrictive != null) {
            responseWriter.writeAllowedRequestHeaders(response, mostRestrictive);
        }

        return false;
    }

}
