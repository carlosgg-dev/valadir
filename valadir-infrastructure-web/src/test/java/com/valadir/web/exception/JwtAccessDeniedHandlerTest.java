package com.valadir.web.exception;

import com.valadir.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class JwtAccessDeniedHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AccessDeniedException exception;

    @Mock
    private SecurityErrorResponseWriter responseWriter;

    @InjectMocks
    private JwtAccessDeniedHandler handler;

    @Test
    void handle_forbiddenRequest_delegates403ToResponseWriter() throws Exception {

        handler.handle(request, response, exception);

        then(responseWriter).should().write(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.ACCESS_DENIED);
        then(responseWriter).shouldHaveNoMoreInteractions();
    }
}
