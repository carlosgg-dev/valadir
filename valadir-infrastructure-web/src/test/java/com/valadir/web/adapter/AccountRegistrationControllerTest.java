package com.valadir.web.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valadir.application.command.ActivateAccountCommand;
import com.valadir.application.command.RegisterCommand;
import com.valadir.application.command.ResendAccountActivationCodeCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.ActivateAccountUseCase;
import com.valadir.application.port.in.RegisterUseCase;
import com.valadir.application.port.in.ResendAccountActivationCodeUseCase;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.PlainOtp;
import com.valadir.domain.model.RawPassword;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.config.LocaleConfig;
import com.valadir.web.config.SecurityConfig;
import com.valadir.web.dto.request.ActivateAccountRequest;
import com.valadir.web.dto.request.RegisterRequest;
import com.valadir.web.dto.request.ResendAccountActivationCodeRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountRegistrationController.class)
@Import({SecurityConfig.class, LocaleConfig.class})
@ExtendWith(MockitoExtension.class)
class AccountRegistrationControllerTest {

    private static final String REQUESTED_LANGUAGE_TAG = "es-ES";
    private static final String UNSUPPORTED_LANGUAGE_TAG = "fr-FR";
    // Not sent by anyone: what LocaleConfig answers when the client states no preference.
    private static final String FALLBACK_LANGUAGE_TAG = "en";
    // A language we do write: if the server's own locale leaked in, the reader would silently
    // start getting Spanish mail they never asked for.
    private static final Locale SERVER_LOCALE = Locale.forLanguageTag("es-ES");

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

    // givenName is optional
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "Batman")
    void register_validRequest_returns201(String givenName) throws Exception {

        var email = Email.from("bruce.wayne@emailValue.com");
        var rawPassword = RawPassword.from("S3cur3P@ss!");
        var fullName = FullName.from("Bruce Wayne");

        var request = new RegisterRequest(email.value(), rawPassword.value(), fullName.value(), givenName);
        var command = new RegisterCommand(email.value(), rawPassword.value(), fullName.value(), givenName, REQUESTED_LANGUAGE_TAG);

        mockMvc.perform(post(ApiRoutes.Auth.Registration.REGISTER_PATH)
                            .header(HttpHeaders.ACCEPT_LANGUAGE, REQUESTED_LANGUAGE_TAG)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        then(registerUseCase).should().register(command);
    }

    // The adapter only carries the tag. Which languages we actually write is the domain's call, so
    // a filter added here would put that list in two places with no way to keep them in sync.
    @Test
    void register_unsupportedAcceptLanguage_passesTheTagThroughUntouched() throws Exception {

        var request = new RegisterRequest("bruce.wayne@emailValue.com", "S3cur3P@ss!", "Bruce Wayne", "Batman");
        var command = new RegisterCommand(request.email(), request.password(), request.fullName(), request.givenName(), UNSUPPORTED_LANGUAGE_TAG);

        mockMvc.perform(post(ApiRoutes.Auth.Registration.REGISTER_PATH)
                            .header(HttpHeaders.ACCEPT_LANGUAGE, UNSUPPORTED_LANGUAGE_TAG)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        then(registerUseCase).should().register(command);
    }

    @Test
    void register_noAcceptLanguageHeader_fallsBackToEnglishNotTheServerLocale() throws Exception {

        var request = new RegisterRequest("bruce.wayne@emailValue.com", "S3cur3P@ss!", "Bruce Wayne", "Batman");
        var command = new RegisterCommand(request.email(), request.password(), request.fullName(), request.givenName(), FALLBACK_LANGUAGE_TAG);

        mockMvc.perform(post(ApiRoutes.Auth.Registration.REGISTER_PATH)
                            .with(runningOnAServerIn(SERVER_LOCALE))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        then(registerUseCase).should().register(command);
    }

    @Test
    void register_blankEmail_returns400() throws Exception {

        var request = new RegisterRequest("", "S3cur3P@ss!", "Bruce Wayne", "Batman");

        mockMvc.perform(post(ApiRoutes.Auth.Registration.REGISTER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(registerUseCase).should(never()).register(any(RegisterCommand.class));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {

        var request = new RegisterRequest("invalid-email", "S3cur3P@ss!", "Bruce Wayne", "Batman");

        mockMvc.perform(post(ApiRoutes.Auth.Registration.REGISTER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(registerUseCase).should(never()).register(any(RegisterCommand.class));
    }

    @Test
    void register_blankPassword_returns400() throws Exception {

        var request = new RegisterRequest("bruce.wayne@emailValue.com", "", "Bruce Wayne", "Batman");

        mockMvc.perform(post(ApiRoutes.Auth.Registration.REGISTER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(registerUseCase).should(never()).register(any(RegisterCommand.class));
    }

    @Test
    void register_blankFullName_returns400() throws Exception {

        var request = new RegisterRequest("bruce.wayne@emailValue.com", "S3cur3P@ss!", "", "Batman");

        mockMvc.perform(post(ApiRoutes.Auth.Registration.REGISTER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(registerUseCase).should(never()).register(any(RegisterCommand.class));
    }

    @Test
    void register_emailAlreadyExists_returns409() throws Exception {

        var request = new RegisterRequest("bruce.wayne@emailValue.com", "S3cur3P@ss!", "Bruce Wayne", "Batman");

        willThrow(new ApplicationException("Email already exists", ErrorCode.EMAIL_ALREADY_EXISTS))
            .given(registerUseCase).register(any(RegisterCommand.class));

        mockMvc.perform(post(ApiRoutes.Auth.Registration.REGISTER_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_ALREADY_EXISTS.getCode()));
    }

    @Test
    void activateAccount_validRequest_returns204() throws Exception {

        var email = Email.from("bruce.wayne@emailValue.com");
        var code = PlainOtp.from("123456");

        var request = new ActivateAccountRequest(email.value(), code.value());
        var command = new ActivateAccountCommand(email.value(), code.value());

        mockMvc.perform(post(ApiRoutes.Auth.Registration.ACTIVATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        then(activateAccountUseCase).should().activate(command);
    }

    @Test
    void activateAccount_blankEmail_returns400() throws Exception {

        var request = new ActivateAccountRequest("", "123456");

        mockMvc.perform(post(ApiRoutes.Auth.Registration.ACTIVATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(activateAccountUseCase).should(never()).activate(any(ActivateAccountCommand.class));
    }

    @Test
    void activateAccount_blankCode_returns400() throws Exception {

        var request = new ActivateAccountRequest("bruce.wayne@emailValue.com", "");

        mockMvc.perform(post(ApiRoutes.Auth.Registration.ACTIVATE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(activateAccountUseCase).should(never()).activate(any(ActivateAccountCommand.class));
    }

    @Test
    void resendAccountActivationCode_validRequest_returns204() throws Exception {

        var email = Email.from("bruce.wayne@emailValue.com");

        var request = new ResendAccountActivationCodeRequest(email.value());
        var command = new ResendAccountActivationCodeCommand(email.value());

        mockMvc.perform(post(ApiRoutes.Auth.Registration.RESEND_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        then(resendAccountActivationCodeUseCase).should().resend(command);
    }

    @Test
    void resendAccountActivationCode_blankEmail_returns400() throws Exception {

        var request = new ResendAccountActivationCodeRequest("");

        mockMvc.perform(post(ApiRoutes.Auth.Registration.RESEND_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()));

        then(resendAccountActivationCodeUseCase).should(never()).resend(any(ResendAccountActivationCodeCommand.class));
    }

    /**
     * What a servlet container hands a request that carries no Accept-Language: getLocale() answers
     * with the server's own locale. Without setting one, MockMvc answers English on its own and this
     * test would pass whether or not LocaleConfig exists.
     */
    private static RequestPostProcessor runningOnAServerIn(Locale serverLocale) {

        return request -> {
            request.setPreferredLocales(List.of(serverLocale));
            request.removeHeader(HttpHeaders.ACCEPT_LANGUAGE);
            return request;
        };
    }
}
