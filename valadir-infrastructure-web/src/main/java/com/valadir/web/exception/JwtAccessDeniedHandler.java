package com.valadir.web.exception;

import com.valadir.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);

    private final SecurityErrorResponseWriter responseWriter;

    public JwtAccessDeniedHandler(final SecurityErrorResponseWriter responseWriter) {

        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final AccessDeniedException e
    ) throws IOException {

        log.warn("Access denied: {}", e.getMessage());

        responseWriter.write(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.ACCESS_DENIED);
    }
}
