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
import com.valadir.domain.model.Email;
import com.valadir.domain.model.PlainOtp;
import com.valadir.domain.model.RawPassword;
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

        var email = Email.from("bruce.wayne@email.com");

        var request = new InitiatePasswordResetRequest(email.value());
        var command = new InitiatePasswordResetCommand(email);

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.INITIATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        then(initiatePasswordResetUseCase).should().initiate(command);
    }

    @Test
    void initiatePasswordReset_blankEmail_returns400() throws Exception {

        var request = new InitiatePasswordResetRequest("");

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.INITIATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(initiatePasswordResetUseCase).should(never()).initiate(any(InitiatePasswordResetCommand.class));
    }

    @Test
    void initiatePasswordReset_invalidEmail_returns400() throws Exception {

        var request = new InitiatePasswordResetRequest("invalid-email");

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.INITIATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(initiatePasswordResetUseCase).should(never()).initiate(any(InitiatePasswordResetCommand.class));
    }

    @Test
    void verifyPasswordResetOtp_validRequest_returns200WithVerificationToken() throws Exception {

        var email = Email.from("bruce.wayne@email.com");
        var resetCode = "718304";
        var verificationToken = "verification-token";

        var request = new VerifyPasswordResetOtpRequest(email.value(), resetCode);
        var command = new VerifyPasswordResetOtpCommand(email, PlainOtp.from(resetCode));

        given(verifyPasswordResetOtpUseCase.verify(command))
            .willReturn(new PasswordResetOtpVerificationResult(verificationToken));

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.VERIFY_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verificationToken").value(verificationToken));
    }

    @Test
    void verifyPasswordResetOtp_blankEmail_returns400() throws Exception {

        var request = new VerifyPasswordResetOtpRequest("", "718304");

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.VERIFY_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(verifyPasswordResetOtpUseCase).should(never()).verify(any(VerifyPasswordResetOtpCommand.class));
    }

    @Test
    void verifyPasswordResetOtp_invalidEmail_returns400() throws Exception {

        var request = new VerifyPasswordResetOtpRequest("invalid-email", "718304");

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.VERIFY_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(verifyPasswordResetOtpUseCase).should(never()).verify(any(VerifyPasswordResetOtpCommand.class));
    }

    @Test
    void verifyPasswordResetOtp_blankCode_returns400() throws Exception {

        var request = new VerifyPasswordResetOtpRequest("bruce.wayne@email.com", "");

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.VERIFY_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(verifyPasswordResetOtpUseCase).should(never()).verify(any(VerifyPasswordResetOtpCommand.class));
    }

    @Test
    void completePasswordReset_validRequest_returns204() throws Exception {

        var verificationToken = "verification-token";
        var password = RawPassword.from("S3cur3P@ss!");

        var request = new CompletePasswordResetRequest(verificationToken, password.value());
        var command = new CompletePasswordResetCommand(verificationToken, password);

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.COMPLETE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        then(completePasswordResetUseCase).should().complete(command);
    }

    @Test
    void completePasswordReset_blankVerificationToken_returns400() throws Exception {

        var request = new CompletePasswordResetRequest("", "S3cur3P@ss!");

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.COMPLETE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(completePasswordResetUseCase).should(never()).complete(any(CompletePasswordResetCommand.class));
    }

    @Test
    void completePasswordReset_blankPassword_returns400() throws Exception {

        var request = new CompletePasswordResetRequest("verification-token", "");

        mockMvc.perform(post(ApiRoutes.Auth.PasswordReset.COMPLETE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(completePasswordResetUseCase).should(never()).complete(any(CompletePasswordResetCommand.class));
    }
}
