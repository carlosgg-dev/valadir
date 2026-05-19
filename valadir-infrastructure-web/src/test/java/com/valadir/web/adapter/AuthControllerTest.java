package com.valadir.web.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.application.command.ActivateAccountCommand;
import com.valadir.application.command.CompletePasswordResetCommand;
import com.valadir.application.command.InitiatePasswordResetCommand;
import com.valadir.application.command.LoginCommand;
import com.valadir.application.command.LogoutCommand;
import com.valadir.application.command.RefreshTokenCommand;
import com.valadir.application.command.RegisterCommand;
import com.valadir.application.command.ResendAccountActivationCodeCommand;
import com.valadir.application.command.VerifyPasswordResetOtpCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.ActivateAccountUseCase;
import com.valadir.application.port.in.CompletePasswordResetUseCase;
import com.valadir.application.port.in.InitiatePasswordResetUseCase;
import com.valadir.application.port.in.LoginUseCase;
import com.valadir.application.port.in.LogoutUseCase;
import com.valadir.application.port.in.RefreshTokenUseCase;
import com.valadir.application.port.in.RegisterUseCase;
import com.valadir.application.port.in.ResendAccountActivationCodeUseCase;
import com.valadir.application.port.in.VerifyPasswordResetOtpUseCase;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.application.result.PasswordResetOtpVerificationResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.config.SecurityConfig;
import com.valadir.web.dto.request.ActivateAccountRequest;
import com.valadir.web.dto.request.CompletePasswordResetRequest;
import com.valadir.web.dto.request.InitiatePasswordResetRequest;
import com.valadir.web.dto.request.LoginRequest;
import com.valadir.web.dto.request.LogoutRequest;
import com.valadir.web.dto.request.RefreshRequest;
import com.valadir.web.dto.request.RegisterRequest;
import com.valadir.web.dto.request.ResendAccountActivationCodeRequest;
import com.valadir.web.dto.request.VerifyPasswordResetOtpRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String ACCESS_TOKEN = "access.token.value";
    private static final String REFRESH_TOKEN = "refresh-token-uuid";
    private static final String ACCOUNT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "S3cur3P@ss!";
    private static final String FULL_NAME = "Bruce Wayne";
    private static final String GIVEN_NAME = "Batman";
    private static final String ACCOUNT_ACTIVATION_CODE = "123456";
    private static final String PASSWORD_RESET_OTP = "718304";
    private static final String PASSWORD_RESET_VERIFICATION_TOKEN = "verification-token";

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
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockitoBean
    private LogoutUseCase logoutUseCase;

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

    @Captor
    private ArgumentCaptor<LogoutCommand> logoutCommandCaptor;

    @Test
    void register_validRequest_returns201() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.REGISTER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest(EMAIL, PASSWORD, FULL_NAME, GIVEN_NAME))))
            .andExpect(status().isCreated());

        then(registerUseCase).should().register(new RegisterCommand(EMAIL, PASSWORD, FULL_NAME, GIVEN_NAME));
    }

    @Test
    void register_blankEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.REGISTER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest("", PASSWORD, FULL_NAME, GIVEN_NAME))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(registerUseCase).should(never()).register(any(RegisterCommand.class));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.REGISTER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest("invalid-email", PASSWORD, FULL_NAME, GIVEN_NAME))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(registerUseCase).should(never()).register(any(RegisterCommand.class));
    }

    @Test
    void register_blankPassword_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.REGISTER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest(EMAIL, "", FULL_NAME, GIVEN_NAME))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(registerUseCase).should(never()).register(any(RegisterCommand.class));
    }

    @Test
    void register_blankFullName_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.REGISTER_PATH)
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

        mockMvc.perform(post(ApiRoutes.Auth.REGISTER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest(EMAIL, PASSWORD, FULL_NAME, GIVEN_NAME))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_ALREADY_EXISTS.getCode()));
    }

    @Test
    void activateAccount_validRequest_returns204() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.AccountActivation.ACTIVATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ActivateAccountRequest(EMAIL, ACCOUNT_ACTIVATION_CODE))))
            .andExpect(status().isNoContent());

        then(activateAccountUseCase).should().activate(new ActivateAccountCommand(EMAIL, ACCOUNT_ACTIVATION_CODE));
    }

    @Test
    void activateAccount_blankEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.AccountActivation.ACTIVATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ActivateAccountRequest("", ACCOUNT_ACTIVATION_CODE))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(activateAccountUseCase).should(never()).activate(any(ActivateAccountCommand.class));
    }

    @Test
    void activateAccount_blankCode_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.AccountActivation.ACTIVATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ActivateAccountRequest(EMAIL, ""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(activateAccountUseCase).should(never()).activate(any(ActivateAccountCommand.class));
    }

    @Test
    void resendAccountActivationCode_validRequest_returns204() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.AccountActivation.RESEND_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ResendAccountActivationCodeRequest(EMAIL))))
            .andExpect(status().isNoContent());

        then(resendAccountActivationCodeUseCase).should().resend(new ResendAccountActivationCodeCommand(EMAIL));
    }

    @Test
    void resendAccountActivationCode_blankEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.AccountActivation.RESEND_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ResendAccountActivationCodeRequest(""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(resendAccountActivationCodeUseCase).should(never()).resend(any(ResendAccountActivationCodeCommand.class));
    }

    @Test
    void login_validCredentials_returns200WithTokens() throws Exception {

        given(loginUseCase.login(new LoginCommand(EMAIL, PASSWORD))).willReturn(new AuthTokenResult(ACCESS_TOKEN, REFRESH_TOKEN));

        mockMvc.perform(post(ApiRoutes.Auth.LOGIN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
            .andExpect(jsonPath("$.refreshToken").value(REFRESH_TOKEN));
    }

    @Test
    void login_badCredentials_returns400() throws Exception {

        willThrow(new ApplicationException("Invalid credentials", ErrorCode.CREDENTIAL_INTEGRITY_ERROR))
            .given(loginUseCase).login(any());

        mockMvc.perform(post(ApiRoutes.Auth.LOGIN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode()));
    }

    @Test
    void login_blankEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.LOGIN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("", PASSWORD))))
            .andExpect(status().isBadRequest());

        then(loginUseCase).should(never()).login(any(LoginCommand.class));
    }

    @Test
    void login_invalidEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.LOGIN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("invalid-email", PASSWORD))))
            .andExpect(status().isBadRequest());

        then(loginUseCase).should(never()).login(any(LoginCommand.class));
    }

    @Test
    void login_blankPassword_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.LOGIN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, ""))))
            .andExpect(status().isBadRequest());

        then(loginUseCase).should(never()).login(any(LoginCommand.class));
    }

    @Test
    void refresh_validToken_returns200WithTokens() throws Exception {

        given(refreshTokenUseCase.refresh(new RefreshTokenCommand(REFRESH_TOKEN))).willReturn(new AuthTokenResult(ACCESS_TOKEN, REFRESH_TOKEN));

        mockMvc.perform(post(ApiRoutes.Auth.REFRESH_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshRequest(REFRESH_TOKEN))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
            .andExpect(jsonPath("$.refreshToken").value(REFRESH_TOKEN));
    }

    @Test
    void refresh_blankToken_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.REFRESH_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshRequest(""))))
            .andExpect(status().isBadRequest());

        then(refreshTokenUseCase).should(never()).refresh(any(RefreshTokenCommand.class));
    }

    @Test
    void refresh_invalidToken_returns401() throws Exception {

        willThrow(new ApplicationException("Invalid token", ErrorCode.INVALID_TOKEN))
            .given(refreshTokenUseCase).refresh(any());

        mockMvc.perform(post(ApiRoutes.Auth.REFRESH_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshRequest(REFRESH_TOKEN))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_TOKEN.getCode()));
    }

    @Test
    void logout_authenticated_returns204() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.LOGOUT_PATH)
                            .with(jwt().jwt(jwt -> jwt
                                .subject(ACCOUNT_ID)
                                .claim("jti", "jti-value")
                                .expiresAt(java.time.Instant.now().plusSeconds(900)))
                            )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LogoutRequest(REFRESH_TOKEN))))
            .andExpect(status().isNoContent());

        then(logoutUseCase).should().logout(logoutCommandCaptor.capture());

        var command = logoutCommandCaptor.getValue();
        assertThat(command.accessTokenJti()).isEqualTo("jti-value");
        assertThat(command.refreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(command.accessTokenRemainingTtl()).isPositive();
        assertThat(command.accountId()).isEqualTo(ACCOUNT_ID);
    }

    @Test
    void logout_blankRefreshToken_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.LOGOUT_PATH)
                            .with(jwt().jwt(jwt -> jwt
                                .subject(ACCOUNT_ID)
                                .claim("jti", "jti-value")
                                .expiresAt(java.time.Instant.now().plusSeconds(900)))
                            )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LogoutRequest(""))))
            .andExpect(status().isBadRequest());

        then(logoutUseCase).should(never()).logout(any(LogoutCommand.class));
    }

    @Test
    void logout_unauthenticated_returns401() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.LOGOUT_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LogoutRequest(REFRESH_TOKEN))))
            .andExpect(status().isUnauthorized());

        then(logoutUseCase).should(never()).logout(any(LogoutCommand.class));
    }

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
