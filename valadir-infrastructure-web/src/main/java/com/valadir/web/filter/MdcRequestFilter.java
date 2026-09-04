package com.valadir.web.filter;

import com.valadir.common.mdc.MdcKeys;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MdcRequestFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String CAPTURED_CONTEXT = MdcRequestFilter.class.getName() + ".capturedContext";

    /**
     * The dispatch to {@code /error} happens once the REQUEST chain has unwound and cleared the MDC,
     * and Spring skips error dispatches by default: the 5xx would be logged with nothing tying it to
     * its request. Hence the capture below, replayed here — path included, which is {@code /error}
     * on this dispatch.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {

        return false;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain
    ) throws ServletException, IOException {

        // Second dispatch over the same request: opening a fresh context here would mint a request id
        // the client never saw, and log the error against /error instead of the path that failed.
        if (request.getDispatcherType() == DispatcherType.ERROR) {
            replayCapturedContext(request);
        } else {
            openContext(request, response);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            request.setAttribute(CAPTURED_CONTEXT, MDC.getCopyOfContextMap());
            MDC.clear();
        }
    }

    private void replayCapturedContext(HttpServletRequest request) {

        if (request.getAttribute(CAPTURED_CONTEXT) instanceof Map<?, ?> captured) {
            captured.forEach((key, value) -> MDC.put(String.valueOf(key), String.valueOf(value)));
        }
    }

    private void openContext(HttpServletRequest request, HttpServletResponse response) {

        String requestId = Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER))
            .filter(id -> !id.isBlank())
            .orElseGet(() -> UUID.randomUUID().toString());

        MDC.put(MdcKeys.REQUEST_ID, requestId);
        MDC.put(MdcKeys.METHOD, request.getMethod());
        MDC.put(MdcKeys.PATH, request.getRequestURI());
        MDC.put(MdcKeys.ACCOUNT_ID, MdcKeys.UNKNOWN);
        response.setHeader(REQUEST_ID_HEADER, requestId);
    }
}
