package com.valadir.web.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.common.error.ErrorCode;
import com.valadir.web.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorResponseWriterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SecurityErrorResponseWriter writer = new SecurityErrorResponseWriter(OBJECT_MAPPER);

    @Test
    void write_setsStatusContentTypeAndErrorCodeBody() throws Exception {

        final var response = new MockHttpServletResponse();
        int status = HttpServletResponse.SC_UNAUTHORIZED;
        ErrorCode errorCode = ErrorCode.AUTHENTICATION_REQUIRED;

        writer.write(response, status, errorCode);

        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

        final ErrorResponse body = OBJECT_MAPPER.readValue(response.getContentAsString(), ErrorResponse.class);
        assertThat(body.code()).isEqualTo(errorCode.getCode());
    }
}
