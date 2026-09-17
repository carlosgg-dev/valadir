package com.valadir.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.common.ratelimit.RateLimitSubject;
import com.valadir.web.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.security.Principal;
import java.util.Locale;
import java.util.Optional;

import static java.util.function.Predicate.not;

public class RateLimitSubjectResolver {

    private static final Logger log = LoggerFactory.getLogger(RateLimitSubjectResolver.class);

    private final ObjectMapper objectMapper;

    public RateLimitSubjectResolver(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    public Optional<RateLimitSubject> resolve(HttpServletRequest request, RateLimitProperties.Rule rule) {

        return switch (rule.strategy()) {
            // The address the container reports, never a header: a caller that picks its own
            // value owns its own bucket, and mints a new one per request it wants unmetered.
            case IP -> Optional.of(subjectOf(rule, request.getRemoteAddr()));
            case EMAIL -> extractEmail(request).map(email -> subjectOf(rule, email));
            case USER -> resolveAccountId().map(accountId -> subjectOf(rule, accountId));
        };
    }

    private RateLimitSubject subjectOf(RateLimitProperties.Rule rule, String value) {

        return new RateLimitSubject(rule.strategy(), rule.path(), value);
    }

    // Normalized the same way Email does: keying on the raw value would let
    // a case change reset the counter and slip past every per-email limit.
    private Optional<String> extractEmail(HttpServletRequest request) {

        try {

            String email = objectMapper.readTree(request.getInputStream()).path("email").asText(null);
            return Optional.ofNullable(email)
                .filter(not(String::isBlank))
                .map(value -> value.trim().toLowerCase(Locale.ROOT));

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
