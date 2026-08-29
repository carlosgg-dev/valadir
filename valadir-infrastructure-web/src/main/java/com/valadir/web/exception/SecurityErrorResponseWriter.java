package com.valadir.web.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.common.error.ErrorCode;
import com.valadir.web.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;
    private final HttpStatusResolver httpStatusResolver;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper, HttpStatusResolver httpStatusResolver) {

        this.objectMapper = objectMapper;
        this.httpStatusResolver = httpStatusResolver;
    }

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {

        response.setStatus(httpStatusResolver.resolve(errorCode).value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(errorCode.getCode()));
    }
}
