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

    private final SecurityErrorResponseWriter writer = new SecurityErrorResponseWriter(OBJECT_MAPPER, new HttpStatusResolver());

    @Test
    void write_setsStatusContentTypeAndErrorCodeBody() throws Exception {

        var response = new MockHttpServletResponse();
        ErrorCode errorCode = ErrorCode.AUTHENTICATION_REQUIRED;

        writer.write(response, errorCode);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = OBJECT_MAPPER.readValue(response.getContentAsString(), ErrorResponse.class);
        assertThat(body.code()).isEqualTo(errorCode.getCode());
    }
}
