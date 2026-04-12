package com.valadir.web.exception;

import com.valadir.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationEntryPointTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthenticationException exception;

    @Mock
    private SecurityErrorResponseWriter responseWriter;

    @InjectMocks
    private JwtAuthenticationEntryPoint entryPoint;

    @Test
    void commence_unauthenticatedRequest_delegates401ToResponseWriter() throws Exception {

        entryPoint.commence(request, response, exception);

        then(responseWriter).should().write(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.AUTHENTICATION_REQUIRED);
        then(responseWriter).shouldHaveNoMoreInteractions();
    }
}
