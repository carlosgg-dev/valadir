package com.valadir.web.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.application.command.ChangePasswordCommand;
import com.valadir.application.port.in.ChangePasswordUseCase;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.domain.model.AccountId;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.config.SecurityConfig;
import com.valadir.web.dto.request.ChangePasswordRequest;
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
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordChangeController.class)
@Import(SecurityConfig.class)
@ExtendWith(MockitoExtension.class)
class PasswordChangeControllerTest {

    private static final String CURRENT_PASSWORD = "SecureP@ss123";
    private static final String NEW_PASSWORD = "AnotherP@ss456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChangePasswordUseCase changePasswordUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RateLimiter rateLimiter;

    // The account comes from the token, never from the body: there is nothing in the request to point elsewhere.
    @Test
    void changePassword_authenticated_returns204AndChangesThePasswordOfTheToken() throws Exception {

        var accountId = AccountId.generate().value().toString();
        var request = new ChangePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD);

        mockMvc.perform(post(ApiRoutes.Auth.Account.CHANGE_PASSWORD_PATH)
                            .with(jwt().jwt(jwt -> jwt.subject(accountId)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        then(changePasswordUseCase).should().change(new ChangePasswordCommand(accountId, CURRENT_PASSWORD, NEW_PASSWORD));
    }

    @Test
    void changePassword_blankCurrentPassword_returns400() throws Exception {

        var request = new ChangePasswordRequest("", NEW_PASSWORD);

        mockMvc.perform(post(ApiRoutes.Auth.Account.CHANGE_PASSWORD_PATH)
                            .with(jwt().jwt(jwt -> jwt.subject(AccountId.generate().value().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        then(changePasswordUseCase).should(never()).change(any(ChangePasswordCommand.class));
    }

    @Test
    void changePassword_blankNewPassword_returns400() throws Exception {

        var request = new ChangePasswordRequest(CURRENT_PASSWORD, "");

        mockMvc.perform(post(ApiRoutes.Auth.Account.CHANGE_PASSWORD_PATH)
                            .with(jwt().jwt(jwt -> jwt.subject(AccountId.generate().value().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        then(changePasswordUseCase).should(never()).change(any(ChangePasswordCommand.class));
    }

    @Test
    void changePassword_unauthenticated_returns401() throws Exception {

        var request = new ChangePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD);

        mockMvc.perform(post(ApiRoutes.Auth.Account.CHANGE_PASSWORD_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());

        then(changePasswordUseCase).should(never()).change(any(ChangePasswordCommand.class));
    }
}
