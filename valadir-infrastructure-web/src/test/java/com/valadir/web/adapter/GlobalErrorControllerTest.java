package com.valadir.web.adapter;

import com.valadir.common.error.ErrorCode;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.web.config.SecurityConfig;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalErrorController.class)
@Import(SecurityConfig.class)
@ExtendWith(MockitoExtension.class)
class GlobalErrorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RateLimiter rateLimiter;

    @Test
    void handleError_withStatusAttribute_propagatesHttpStatus() throws Exception {

        mockMvc.perform(get("/error")
                            .with(jwt())
                            .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_SERVER_ERROR.getCode()))

            .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void handleError_withUnresolvableStatusAttribute_defaultsTo500() throws Exception {

        mockMvc.perform(get("/error")
                            .with(jwt())
                            .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 999))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_SERVER_ERROR.getCode()))

            .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void handleError_withNoStatusAttribute_defaultsTo500() throws Exception {

        mockMvc.perform(get("/error")
                            .with(jwt()))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_SERVER_ERROR.getCode()))

            .andExpect(jsonPath("$.errors").doesNotExist());
    }
}
