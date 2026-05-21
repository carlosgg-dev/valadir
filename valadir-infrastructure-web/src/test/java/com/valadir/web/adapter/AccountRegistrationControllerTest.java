package com.valadir.web.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.application.command.ActivateAccountCommand;
import com.valadir.application.command.RegisterCommand;
import com.valadir.application.command.ResendAccountActivationCodeCommand;
import com.valadir.application.otp.PlainOtp;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.ActivateAccountUseCase;
import com.valadir.application.port.in.RegisterUseCase;
import com.valadir.application.port.in.ResendAccountActivationCodeUseCase;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.config.SecurityConfig;
import com.valadir.web.dto.request.ActivateAccountRequest;
import com.valadir.web.dto.request.RegisterRequest;
import com.valadir.web.dto.request.ResendAccountActivationCodeRequest;
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
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountRegistrationController.class)
@Import(SecurityConfig.class)
@ExtendWith(MockitoExtension.class)
class AccountRegistrationControllerTest {

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "S3cur3P@ss!";
    private static final String FULL_NAME = "Bruce Wayne";
    private static final String GIVEN_NAME = "Batman";
    private static final String ACCOUNT_ACTIVATION_CODE = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterUseCase registerUseCase;

    @MockitoBean
    private ActivateAccountUseCase activateAccountUseCase;

    @MockitoBean
    private ResendAccountActivationCodeUseCase resendAccountActivationCodeUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RateLimiter rateLimiter;

    @Test
    void register_validRequest_returns201() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Registration.REGISTER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest(EMAIL, PASSWORD, FULL_NAME, GIVEN_NAME))))
            .andExpect(status().isCreated());

        then(registerUseCase).should().register(new RegisterCommand(EMAIL, PASSWORD, FULL_NAME, GIVEN_NAME));
    }

    @Test
    void register_blankEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Registration.REGISTER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest("", PASSWORD, FULL_NAME, GIVEN_NAME))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(registerUseCase).should(never()).register(any(RegisterCommand.class));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Registration.REGISTER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest("invalid-email", PASSWORD, FULL_NAME, GIVEN_NAME))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(registerUseCase).should(never()).register(any(RegisterCommand.class));
    }

    @Test
    void register_blankPassword_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Registration.REGISTER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest(EMAIL, "", FULL_NAME, GIVEN_NAME))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(registerUseCase).should(never()).register(any(RegisterCommand.class));
    }

    @Test
    void register_blankFullName_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Registration.REGISTER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest(EMAIL, PASSWORD, "", GIVEN_NAME))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(registerUseCase).should(never()).register(any(RegisterCommand.class));
    }

    @Test
    void register_emailAlreadyExists_returns409() throws Exception {

        willThrow(new ApplicationException("Email already exists", ErrorCode.EMAIL_ALREADY_EXISTS))
            .given(registerUseCase).register(any(RegisterCommand.class));

        mockMvc.perform(post(ApiRoutes.Auth.Registration.REGISTER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest(EMAIL, PASSWORD, FULL_NAME, GIVEN_NAME))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_ALREADY_EXISTS.getCode()));
    }

    @Test
    void activateAccount_validRequest_returns204() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Registration.ACTIVATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ActivateAccountRequest(EMAIL, ACCOUNT_ACTIVATION_CODE))))
            .andExpect(status().isNoContent());

        then(activateAccountUseCase).should().activate(new ActivateAccountCommand(EMAIL, PlainOtp.from(ACCOUNT_ACTIVATION_CODE)));
    }

    @Test
    void activateAccount_blankEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Registration.ACTIVATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ActivateAccountRequest("", ACCOUNT_ACTIVATION_CODE))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(activateAccountUseCase).should(never()).activate(any(ActivateAccountCommand.class));
    }

    @Test
    void activateAccount_blankCode_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Registration.ACTIVATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ActivateAccountRequest(EMAIL, ""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(activateAccountUseCase).should(never()).activate(any(ActivateAccountCommand.class));
    }

    @Test
    void resendAccountActivationCode_validRequest_returns204() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Registration.RESEND_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ResendAccountActivationCodeRequest(EMAIL))))
            .andExpect(status().isNoContent());

        then(resendAccountActivationCodeUseCase).should().resend(new ResendAccountActivationCodeCommand(EMAIL));
    }

    @Test
    void resendAccountActivationCode_blankEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Registration.RESEND_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ResendAccountActivationCodeRequest(""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(resendAccountActivationCodeUseCase).should(never()).resend(any(ResendAccountActivationCodeCommand.class));
    }
}
