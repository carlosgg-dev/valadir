package com.valadir.web.exception;

import com.valadir.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    private final SecurityErrorResponseWriter responseWriter;

    public JwtAuthenticationEntryPoint(SecurityErrorResponseWriter responseWriter) {

        this.responseWriter = responseWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException {

        log.warn("Authentication failed: {}", e.getMessage());

        responseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.AUTHENTICATION_REQUIRED);
    }
}
