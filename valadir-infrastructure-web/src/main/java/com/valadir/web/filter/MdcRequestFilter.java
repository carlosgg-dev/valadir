package com.valadir.web.filter;

import com.valadir.common.mdc.MdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class MdcRequestFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain
    ) throws ServletException, IOException {

        String requestId = Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER))
            .filter(id -> !id.isBlank())
            .orElseGet(() -> UUID.randomUUID().toString());

        MDC.put(MdcKeys.REQUEST_ID, requestId);
        MDC.put(MdcKeys.METHOD, request.getMethod());
        MDC.put(MdcKeys.PATH, request.getRequestURI());
        MDC.put(MdcKeys.ACCOUNT_ID, MdcKeys.UNKNOWN);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
