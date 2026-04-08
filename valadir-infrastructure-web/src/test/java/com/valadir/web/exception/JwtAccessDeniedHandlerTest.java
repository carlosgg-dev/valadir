package com.valadir.web.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.common.error.ErrorCode;
import com.valadir.web.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class JwtAccessDeniedHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AccessDeniedException exception;

    private JwtAccessDeniedHandler handler;

    private StringWriter responseBody;

    @BeforeEach
    void setUp() throws Exception {

        handler = new JwtAccessDeniedHandler(new ObjectMapper());
        responseBody = new StringWriter();
        given(response.getWriter()).willReturn(new PrintWriter(responseBody));
    }

    @Test
    void handle_forbiddenRequest_returns403WithCode() throws Exception {

        handler.handle(request, response, exception);

        then(response).should().setStatus(HttpServletResponse.SC_FORBIDDEN);
        then(response).should().setContentType(MediaType.APPLICATION_JSON_VALUE);

        final ErrorResponse body = new ObjectMapper().readValue(responseBody.toString(), ErrorResponse.class);
        assertThat(body.code()).isEqualTo(ErrorCode.ACCESS_DENIED.getCode());
    }
}
