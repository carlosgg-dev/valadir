package com.valadir.web.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.application.command.LoginCommand;
import com.valadir.application.command.LogoutCommand;
import com.valadir.application.command.RefreshTokenCommand;
import com.valadir.application.command.RegisterCommand;
import com.valadir.application.command.ResendVerificationCommand;
import com.valadir.application.command.VerifyEmailCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.LoginUseCase;
import com.valadir.application.port.in.LogoutUseCase;
import com.valadir.application.port.in.RefreshTokenUseCase;
import com.valadir.application.port.in.RegisterUseCase;
import com.valadir.application.port.in.ResendVerificationUseCase;
import com.valadir.application.port.in.VerifyEmailUseCase;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.config.SecurityConfig;
import com.valadir.web.dto.request.LoginRequest;
import com.valadir.web.dto.request.LogoutRequest;
import com.valadir.web.dto.request.RefreshRequest;
import com.valadir.web.dto.request.RegisterRequest;
import com.valadir.web.dto.request.ResendVerificationRequest;
import com.valadir.web.dto.request.VerifyEmailRequest;
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
    private static final String VERIFY_CODE = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterUseCase registerUseCase;

    @MockitoBean
    private VerifyEmailUseCase verifyEmailUseCase;

    @MockitoBean
    private ResendVerificationUseCase resendVerificationUseCase;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockitoBean
    private LogoutUseCase logoutUseCase;

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
    void verifyEmail_validRequest_returns204() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.VERIFY_EMAIL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VerifyEmailRequest(EMAIL, VERIFY_CODE))))
            .andExpect(status().isNoContent());

        then(verifyEmailUseCase).should().verify(new VerifyEmailCommand(EMAIL, VERIFY_CODE));
    }

    @Test
    void verifyEmail_blankEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.VERIFY_EMAIL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VerifyEmailRequest("", VERIFY_CODE))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(verifyEmailUseCase).should(never()).verify(any(VerifyEmailCommand.class));
    }

    @Test
    void verifyEmail_blankCode_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.VERIFY_EMAIL_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VerifyEmailRequest(EMAIL, ""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(verifyEmailUseCase).should(never()).verify(any(VerifyEmailCommand.class));
    }

    @Test
    void resendVerification_validRequest_returns204() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.RESEND_VERIFICATION_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ResendVerificationRequest(EMAIL))))
            .andExpect(status().isNoContent());

        then(resendVerificationUseCase).should().resend(new ResendVerificationCommand(EMAIL));
    }

    @Test
    void resendVerification_blankEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.RESEND_VERIFICATION_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ResendVerificationRequest(""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(resendVerificationUseCase).should(never()).resend(any(ResendVerificationCommand.class));
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
                            .content(objectMapper.writeValueAsString(new LoginRequest("bad-email", PASSWORD))))
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
        assertThat(command.accessTokenRemainingTtlSeconds()).isPositive();
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
}
