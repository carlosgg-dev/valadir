package com.valadir.web.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.application.command.CompleteEmailChangeCommand;
import com.valadir.application.command.InitiateEmailChangeCommand;
import com.valadir.application.port.in.CompleteEmailChangeUseCase;
import com.valadir.application.port.in.InitiateEmailChangeUseCase;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.domain.model.AccountId;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.config.SecurityConfig;
import com.valadir.web.dto.request.CompleteEmailChangeRequest;
import com.valadir.web.dto.request.InitiateEmailChangeRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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

@WebMvcTest(EmailChangeController.class)
@Import(SecurityConfig.class)
@ExtendWith(MockitoExtension.class)
class EmailChangeControllerTest {

    private static final String NEW_EMAIL = "matches.malone@email.com";
    private static final String PASSWORD = "SecureP@ss123";
    private static final String CODE = "482913";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InitiateEmailChangeUseCase initiateEmailChangeUseCase;

    @MockitoBean
    private CompleteEmailChangeUseCase completeEmailChangeUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RateLimiter rateLimiter;

    // The account comes from the token, never from the body: there is nothing in the request to point elsewhere.
    @Test
    void initiateEmailChange_authenticated_returns204AndInitiatesTheChangeOfTheToken() throws Exception {

        var accountId = AccountId.generate().value().toString();

        mockMvc.perform(post(ApiRoutes.Auth.Account.INITIATE_EMAIL_CHANGE_PATH)
                            .with(jwt().jwt(jwt -> jwt.subject(accountId)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new InitiateEmailChangeRequest(NEW_EMAIL, PASSWORD))))
            .andExpect(status().isNoContent());

        then(initiateEmailChangeUseCase).should().initiate(new InitiateEmailChangeCommand(accountId, NEW_EMAIL, PASSWORD));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void initiateEmailChange_blankNewEmail_returns400(String newEmail) throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Account.INITIATE_EMAIL_CHANGE_PATH)
                            .with(jwt().jwt(jwt -> jwt.subject(AccountId.generate().value().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new InitiateEmailChangeRequest(newEmail, PASSWORD))))
            .andExpect(status().isBadRequest());

        then(initiateEmailChangeUseCase).should(never()).initiate(any(InitiateEmailChangeCommand.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void initiateEmailChange_blankPassword_returns400(String password) throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Account.INITIATE_EMAIL_CHANGE_PATH)
                            .with(jwt().jwt(jwt -> jwt.subject(AccountId.generate().value().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new InitiateEmailChangeRequest(NEW_EMAIL, password))))
            .andExpect(status().isBadRequest());

        then(initiateEmailChangeUseCase).should(never()).initiate(any(InitiateEmailChangeCommand.class));
    }

    @Test
    void initiateEmailChange_unauthenticated_returns401() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Account.INITIATE_EMAIL_CHANGE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new InitiateEmailChangeRequest(NEW_EMAIL, PASSWORD))))
            .andExpect(status().isUnauthorized());

        then(initiateEmailChangeUseCase).should(never()).initiate(any(InitiateEmailChangeCommand.class));
    }

    @Test
    void completeEmailChange_authenticated_returns204AndCompletesTheChangeOfTheToken() throws Exception {

        var accountId = AccountId.generate().value().toString();

        mockMvc.perform(post(ApiRoutes.Auth.Account.COMPLETE_EMAIL_CHANGE_PATH)
                            .with(jwt().jwt(jwt -> jwt.subject(accountId)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CompleteEmailChangeRequest(CODE))))
            .andExpect(status().isNoContent());

        then(completeEmailChangeUseCase).should().complete(new CompleteEmailChangeCommand(accountId, CODE));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "48291", "4829134", "48a913"})
    void completeEmailChange_malformedCode_returns400(String code) throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Account.COMPLETE_EMAIL_CHANGE_PATH)
                            .with(jwt().jwt(jwt -> jwt.subject(AccountId.generate().value().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CompleteEmailChangeRequest(code))))
            .andExpect(status().isBadRequest());

        then(completeEmailChangeUseCase).should(never()).complete(any(CompleteEmailChangeCommand.class));
    }

    @Test
    void completeEmailChange_unauthenticated_returns401() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Account.COMPLETE_EMAIL_CHANGE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CompleteEmailChangeRequest(CODE))))
            .andExpect(status().isUnauthorized());

        then(completeEmailChangeUseCase).should(never()).complete(any(CompleteEmailChangeCommand.class));
    }
}
