package com.valadir.web.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.application.command.LoginCommand;
import com.valadir.application.command.LogoutCommand;
import com.valadir.application.command.RefreshTokenCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.LoginUseCase;
import com.valadir.application.port.in.LogoutUseCase;
import com.valadir.application.port.in.RefreshTokenUseCase;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.RawPassword;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.config.SecurityConfig;
import com.valadir.web.dto.request.LoginRequest;
import com.valadir.web.dto.request.LogoutRequest;
import com.valadir.web.dto.request.RefreshRequest;
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

@WebMvcTest(SessionController.class)
@Import(SecurityConfig.class)
@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void login_validCredentials_returns200WithTokens() throws Exception {

        var email = Email.from("bruce.wayne@email.com");
        var password = RawPassword.from("S3cur3P@ss!");
        var accessToken = "access.token.value";
        var refreshToken = "refresh-token-uuid";

        given(loginUseCase.login(new LoginCommand(email, password))).willReturn(new AuthTokenResult(accessToken, refreshToken));

        mockMvc.perform(post(ApiRoutes.Auth.Session.LOGIN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(email.value(), password.value()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value(accessToken))
            .andExpect(jsonPath("$.refreshToken").value(refreshToken));
    }

    @Test
    void login_badCredentials_returns400() throws Exception {

        willThrow(new ApplicationException("Invalid credentials", ErrorCode.CREDENTIAL_INTEGRITY_ERROR))
            .given(loginUseCase).login(any());

        mockMvc.perform(post(ApiRoutes.Auth.Session.LOGIN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("bruce.wayne@email.com", "S3cur3P@ss!"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode()));
    }

    @Test
    void login_blankEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Session.LOGIN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("", "S3cur3P@ss!"))))
            .andExpect(status().isBadRequest());

        then(loginUseCase).should(never()).login(any(LoginCommand.class));
    }

    @Test
    void login_invalidEmail_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Session.LOGIN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("invalid-email", "S3cur3P@ss!"))))
            .andExpect(status().isBadRequest());

        then(loginUseCase).should(never()).login(any(LoginCommand.class));
    }

    @Test
    void login_blankPassword_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Session.LOGIN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("bruce.wayne@email.com", ""))))
            .andExpect(status().isBadRequest());

        then(loginUseCase).should(never()).login(any(LoginCommand.class));
    }

    @Test
    void refresh_validToken_returns200WithTokens() throws Exception {

        var accessToken = "access.token.value";
        var refreshToken = "refresh-token-uuid";

        given(refreshTokenUseCase.refresh(new RefreshTokenCommand(refreshToken))).willReturn(new AuthTokenResult(accessToken, refreshToken));

        mockMvc.perform(post(ApiRoutes.Auth.Session.REFRESH_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value(accessToken))
            .andExpect(jsonPath("$.refreshToken").value(refreshToken));
    }

    @Test
    void refresh_blankToken_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Session.REFRESH_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshRequest(""))))
            .andExpect(status().isBadRequest());

        then(refreshTokenUseCase).should(never()).refresh(any(RefreshTokenCommand.class));
    }

    @Test
    void refresh_invalidToken_returns401() throws Exception {

        var refreshToken = "refresh-token-uuid";

        willThrow(new ApplicationException("Invalid token", ErrorCode.INVALID_TOKEN))
            .given(refreshTokenUseCase).refresh(any());

        mockMvc.perform(post(ApiRoutes.Auth.Session.REFRESH_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_TOKEN.getCode()));
    }

    @Test
    void logout_authenticated_returns204() throws Exception {

        var accountId = AccountId.generate();
        var refreshToken = "refresh-token-uuid";

        mockMvc.perform(post(ApiRoutes.Auth.Session.LOGOUT_PATH)
                            .with(jwt().jwt(jwt -> jwt
                                .subject(accountId.value().toString())
                                .claim("jti", "jti-value")
                                .expiresAt(java.time.Instant.now().plusSeconds(900)))
                            )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LogoutRequest(refreshToken))))
            .andExpect(status().isNoContent());

        then(logoutUseCase).should().logout(logoutCommandCaptor.capture());

        var command = logoutCommandCaptor.getValue();
        assertThat(command.accessTokenJti()).isEqualTo("jti-value");
        assertThat(command.refreshToken()).isEqualTo(refreshToken);
        assertThat(command.accessTokenRemainingTtl()).isPositive();
        assertThat(command.accountId()).isEqualTo(accountId);
    }

    @Test
    void logout_blankRefreshToken_returns400() throws Exception {

        var accountId = AccountId.generate();

        mockMvc.perform(post(ApiRoutes.Auth.Session.LOGOUT_PATH)
                            .with(jwt().jwt(jwt -> jwt
                                .subject(accountId.value().toString())
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

        var refreshToken = "refresh-token-uuid";

        mockMvc.perform(post(ApiRoutes.Auth.Session.LOGOUT_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LogoutRequest(refreshToken))))
            .andExpect(status().isUnauthorized());

        then(logoutUseCase).should(never()).logout(any(LogoutCommand.class));
    }
}
