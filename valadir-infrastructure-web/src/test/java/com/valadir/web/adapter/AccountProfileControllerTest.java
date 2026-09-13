package com.valadir.web.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.application.command.UpdateProfileCommand;
import com.valadir.application.port.in.GetProfileUseCase;
import com.valadir.application.port.in.UpdateProfileUseCase;
import com.valadir.application.result.ProfileResult;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.domain.model.AccountId;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.config.SecurityConfig;
import com.valadir.web.dto.request.UpdateProfileRequest;
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
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountProfileController.class)
@Import(SecurityConfig.class)
@ExtendWith(MockitoExtension.class)
class AccountProfileControllerTest {

    private static final ProfileResult PROFILE = new ProfileResult("bruce.wayne@email.com", "Bruce Wayne", "Batman", "es");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GetProfileUseCase getProfileUseCase;

    @MockitoBean
    private UpdateProfileUseCase updateProfileUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RateLimiter rateLimiter;

    @Test
    void getProfile_authenticated_returns200WithTheProfileOfTheToken() throws Exception {

        var accountId = AccountId.generate().value().toString();

        given(getProfileUseCase.getProfile(accountId)).willReturn(PROFILE);

        var response = mockMvc.perform(get(ApiRoutes.Auth.Account.PROFILE_PATH)
                                           .with(jwt().jwt(jwt -> jwt.subject(accountId))));

        expectProfile(response);
    }

    @Test
    void getProfile_unauthenticated_returns401() throws Exception {

        mockMvc.perform(get(ApiRoutes.Auth.Account.PROFILE_PATH))
            .andExpect(status().isUnauthorized());

        then(getProfileUseCase).should(never()).getProfile(any());
    }

    // The account comes from the token, never from the body: there is nothing in the request to point elsewhere.
    @Test
    void updateProfile_authenticated_returns200WithTheUpdatedProfileOfTheToken() throws Exception {

        var accountId = AccountId.generate().value().toString();
        var command = new UpdateProfileCommand(accountId, PROFILE.fullName(), PROFILE.givenName(), PROFILE.language());
        var request = new UpdateProfileRequest(PROFILE.fullName(), PROFILE.givenName(), PROFILE.language());

        given(updateProfileUseCase.update(command))
            .willReturn(PROFILE);

        var response = mockMvc.perform(put(ApiRoutes.Auth.Account.PROFILE_PATH)
                                           .with(jwt().jwt(jwt -> jwt.subject(accountId)))
                                           .contentType(MediaType.APPLICATION_JSON)
                                           .content(objectMapper.writeValueAsString(request)));

        expectProfile(response);
    }

    @Test
    void updateProfile_blankFullName_returns400() throws Exception {

        var request = new UpdateProfileRequest("", PROFILE.givenName(), PROFILE.language());

        mockMvc.perform(put(ApiRoutes.Auth.Account.PROFILE_PATH)
                            .with(jwt().jwt(jwt -> jwt.subject(AccountId.generate().value().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        then(updateProfileUseCase).should(never()).update(any());
    }

    @Test
    void updateProfile_blankLanguage_returns400() throws Exception {

        var request = new UpdateProfileRequest(PROFILE.fullName(), PROFILE.givenName(), "");

        mockMvc.perform(put(ApiRoutes.Auth.Account.PROFILE_PATH)
                            .with(jwt().jwt(jwt -> jwt.subject(AccountId.generate().value().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        then(updateProfileUseCase).should(never()).update(any());
    }

    @Test
    void updateProfile_unauthenticated_returns401() throws Exception {

        var request = new UpdateProfileRequest(PROFILE.fullName(), PROFILE.givenName(), PROFILE.language());

        mockMvc.perform(put(ApiRoutes.Auth.Account.PROFILE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());

        then(updateProfileUseCase).should(never()).update(any());
    }

    private static void expectProfile(ResultActions response) throws Exception {

        response
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(PROFILE.email()))
            .andExpect(jsonPath("$.fullName").value(PROFILE.fullName()))
            .andExpect(jsonPath("$.givenName").value(PROFILE.givenName()))
            .andExpect(jsonPath("$.language").value(PROFILE.language()));
    }
}
