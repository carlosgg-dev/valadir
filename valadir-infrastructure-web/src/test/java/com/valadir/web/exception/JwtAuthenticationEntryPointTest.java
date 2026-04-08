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
import org.springframework.security.core.AuthenticationException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationEntryPointTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthenticationException exception;

    private JwtAuthenticationEntryPoint entryPoint;

    private StringWriter responseBody;

    @BeforeEach
    void setUp() throws Exception {

        entryPoint = new JwtAuthenticationEntryPoint(new ObjectMapper());
        responseBody = new StringWriter();
        given(response.getWriter()).willReturn(new PrintWriter(responseBody));
    }

    @Test
    void commence_unauthenticatedRequest_returns401WithCode() throws Exception {

        entryPoint.commence(request, response, exception);

        then(response).should().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        then(response).should().setContentType(MediaType.APPLICATION_JSON_VALUE);

        final ErrorResponse body = new ObjectMapper().readValue(responseBody.toString(), ErrorResponse.class);
        assertThat(body.code()).isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED.getCode());
    }
}
