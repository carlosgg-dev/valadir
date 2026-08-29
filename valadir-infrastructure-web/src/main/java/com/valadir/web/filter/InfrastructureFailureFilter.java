package com.valadir.web.filter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.web.exception.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Answers 503 when a dependency fails before the request reaches the controller.
 *
 * <p>{@code GlobalExceptionHandler} only sees what the DispatcherServlet dispatches, so an outage
 * raised by a filter — the blacklist lookup, the rate limiter — would otherwise surface as a
 * generic 500.
 */
public class InfrastructureFailureFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureFailureFilter.class);

    private final SecurityErrorResponseWriter responseWriter;

    public InfrastructureFailureFilter(SecurityErrorResponseWriter responseWriter) {

        this.responseWriter = responseWriter;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain
    ) throws ServletException, IOException {

        try {
            chain.doFilter(request, response);
        } catch (InfrastructureException e) {
            log.error("Infrastructure dependency unavailable before the request reached the controller: {}", e.getMessage(), e);
            responseWriter.write(response, e.getErrorCode());
        }
    }
}
