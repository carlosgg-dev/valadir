package com.valadir.web.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.application.command.CompletePasswordResetCommand;
import com.valadir.application.command.InitiatePasswordResetCommand;
import com.valadir.application.command.VerifyPasswordResetOtpCommand;
import com.valadir.application.port.in.CompletePasswordResetUseCase;
import com.valadir.application.port.in.InitiatePasswordResetUseCase;
import com.valadir.application.port.in.VerifyPasswordResetOtpUseCase;
import com.valadir.application.result.PasswordResetOtpVerificationResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.config.SecurityConfig;
import com.valadir.web.dto.request.CompletePasswordResetRequest;
import com.valadir.web.dto.request.InitiatePasswordResetRequest;
import com.valadir.web.dto.request.VerifyPasswordResetOtpRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetController.class)
@Import(SecurityConfig.class)
@ExtendWith(MockitoExtension.class)
class PasswordResetControllerTest {

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "S3cur3P@ss!";
    private static final String PASSWORD_RESET_OTP = "718304";
    private static final String PASSWORD_RESET_VERIFICATION_TOKEN = "verification-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InitiatePasswordResetUseCase initiatePasswordResetUseCase;

    @MockitoBean
    private VerifyPasswordResetOtpUseCase verifyPasswordResetOtpUseCase;

    @MockitoBean
    private CompletePasswordResetUseCase completePasswordResetUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RateLimiter rateLimiter;

    @Test
    void initiatePasswordReset_validRequest_returns204() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.INITIATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new InitiatePasswordResetRequest(EMAIL))))
            .andExpect(status().isNoContent());

        then(initiatePasswordResetUseCase).should().initiate(new InitiatePasswordResetCommand(EMAIL));
    }

    @Test
    void initiatePasswordReset_blankEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.INITIATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new InitiatePasswordResetRequest(""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(initiatePasswordResetUseCase).should(never()).initiate(any(InitiatePasswordResetCommand.class));
    }

    @Test
    void initiatePasswordReset_invalidEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.INITIATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new InitiatePasswordResetRequest("invalid-email"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(initiatePasswordResetUseCase).should(never()).initiate(any(InitiatePasswordResetCommand.class));
    }

    @Test
    void verifyPasswordResetOtp_validRequest_returns200WithVerificationToken() throws Exception {

        given(verifyPasswordResetOtpUseCase.verify(new VerifyPasswordResetOtpCommand(EMAIL, PASSWORD_RESET_OTP)))
            .willReturn(new PasswordResetOtpVerificationResult(PASSWORD_RESET_VERIFICATION_TOKEN));

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.VERIFY_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VerifyPasswordResetOtpRequest(EMAIL, PASSWORD_RESET_OTP))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verificationToken").value(PASSWORD_RESET_VERIFICATION_TOKEN));
    }

    @Test
    void verifyPasswordResetOtp_blankEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.VERIFY_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VerifyPasswordResetOtpRequest("", PASSWORD_RESET_OTP))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(verifyPasswordResetOtpUseCase).should(never()).verify(any(VerifyPasswordResetOtpCommand.class));
    }

    @Test
    void verifyPasswordResetOtp_invalidEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.VERIFY_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VerifyPasswordResetOtpRequest("invalid-email", PASSWORD_RESET_OTP))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(verifyPasswordResetOtpUseCase).should(never()).verify(any(VerifyPasswordResetOtpCommand.class));
    }

    @Test
    void verifyPasswordResetOtp_blankCode_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.VERIFY_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VerifyPasswordResetOtpRequest(EMAIL, ""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(verifyPasswordResetOtpUseCase).should(never()).verify(any(VerifyPasswordResetOtpCommand.class));
    }

    @Test
    void completePasswordReset_validRequest_returns204() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.COMPLETE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CompletePasswordResetRequest(PASSWORD_RESET_VERIFICATION_TOKEN, PASSWORD))))
            .andExpect(status().isNoContent());

        then(completePasswordResetUseCase).should().complete(new CompletePasswordResetCommand(PASSWORD_RESET_VERIFICATION_TOKEN, PASSWORD));
    }

    @Test
    void completePasswordReset_blankVerificationToken_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.COMPLETE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CompletePasswordResetRequest("", PASSWORD))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(completePasswordResetUseCase).should(never()).complete(any(CompletePasswordResetCommand.class));
    }

    @Test
    void completePasswordReset_blankPassword_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.COMPLETE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CompletePasswordResetRequest(PASSWORD_RESET_VERIFICATION_TOKEN, ""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(completePasswordResetUseCase).should(never()).complete(any(CompletePasswordResetCommand.class));
    }
}
