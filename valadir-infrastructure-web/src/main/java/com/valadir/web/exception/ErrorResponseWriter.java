package com.valadir.web.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.common.error.ErrorCode;
import com.valadir.web.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;
    private final HttpStatusResolver httpStatusResolver;

    public ErrorResponseWriter(ObjectMapper objectMapper, HttpStatusResolver httpStatusResolver) {

        this.objectMapper = objectMapper;
        this.httpStatusResolver = httpStatusResolver;
    }

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {

        response.setStatus(httpStatusResolver.resolve(errorCode).value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // Through the stream, not the writer: the writer appends the container's default charset,
        // and JSON is UTF-8 by definition.
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(errorCode.getCode()));
    }
}
