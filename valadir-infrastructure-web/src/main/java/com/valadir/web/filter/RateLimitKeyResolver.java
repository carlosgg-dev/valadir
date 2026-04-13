package com.valadir.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.security.redis.RedisKeySpace;
import com.valadir.web.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.function.Predicate.not;

public class RateLimitKeyResolver {

    private static final Logger log = LoggerFactory.getLogger(RateLimitKeyResolver.class);
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private final ObjectMapper objectMapper;

    public RateLimitKeyResolver(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    public Optional<String> resolve(HttpServletRequest request, RateLimitProperties.Rule rule) {

        return switch (rule.strategy()) {
            case IP -> Optional.of(RedisKeySpace.forRateLimitIp(normalizePathSegment(rule.path()), resolveIp(request)));
            case EMAIL -> extractEmail(request).map(email -> RedisKeySpace.forRateLimitEmail(normalizePathSegment(rule.path()), email));
            case USER -> resolveAccountId().map(RedisKeySpace::forRateLimitUser);
        };
    }

    private String normalizePathSegment(String path) {

        String normalized = NON_ALPHANUMERIC.matcher(path.toLowerCase()).replaceAll("_");
        return normalized.replaceAll("^_+", "").replaceAll("_+$", "");
    }

    private String resolveIp(HttpServletRequest request) {

        String forwarded = request.getHeader("X-Forwarded-For");

        return forwarded != null && !forwarded.isBlank()
            ? forwarded.split(",")[0].trim()
            : request.getRemoteAddr();
    }

    private Optional<String> extractEmail(HttpServletRequest request) {

        try {
            String email = objectMapper.readTree(request.getInputStream()).path("email").asText(null);
            return Optional.ofNullable(email).filter(not(String::isBlank));
        } catch (IOException e) {
            log.warn("Could not extract email from request body for rate limiting", e);
            return Optional.empty();
        }
    }

    private Optional<String> resolveAccountId() {

        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .filter(JwtAuthenticationToken.class::isInstance)
            .map(Principal::getName);
    }
}
