package com.valadir.web.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.application.command.DeleteAccountCommand;
import com.valadir.application.port.in.DeleteAccountUseCase;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.domain.model.AccountId;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.config.SecurityConfig;
import com.valadir.web.dto.request.DeleteAccountRequest;
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

@WebMvcTest(AccountDeletionController.class)
@Import(SecurityConfig.class)
@ExtendWith(MockitoExtension.class)
class AccountDeletionControllerTest {

    private static final String PASSWORD = "SecureP@ss123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeleteAccountUseCase deleteAccountUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RateLimiter rateLimiter;

    // The account comes from the token, never from the body: there is nothing in the request to point elsewhere.
    @Test
    void deleteAccount_authenticated_returns204AndDeletesTheAccountOfTheToken() throws Exception {

        var accountId = AccountId.generate().value().toString();

        mockMvc.perform(post(ApiRoutes.Auth.Account.DELETE_PATH)
                            .with(jwt().jwt(jwt -> jwt.subject(accountId)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DeleteAccountRequest(PASSWORD))))
            .andExpect(status().isNoContent());

        then(deleteAccountUseCase).should().delete(new DeleteAccountCommand(accountId, PASSWORD));
    }

    @Test
    void deleteAccount_blankPassword_returns400() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Account.DELETE_PATH)
                            .with(jwt().jwt(jwt -> jwt.subject(AccountId.generate().value().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DeleteAccountRequest(""))))
            .andExpect(status().isBadRequest());

        then(deleteAccountUseCase).should(never()).delete(any(DeleteAccountCommand.class));
    }

    @Test
    void deleteAccount_unauthenticated_returns401() throws Exception {

        mockMvc.perform(post(ApiRoutes.Auth.Account.DELETE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DeleteAccountRequest(PASSWORD))))
            .andExpect(status().isUnauthorized());

        then(deleteAccountUseCase).should(never()).delete(any(DeleteAccountCommand.class));
    }
}
