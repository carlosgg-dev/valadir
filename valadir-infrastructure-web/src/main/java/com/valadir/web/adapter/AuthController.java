package com.valadir.web.adapter;

import com.valadir.application.command.ActivateAccountCommand;
import com.valadir.application.command.CompletePasswordResetCommand;
import com.valadir.application.command.InitiatePasswordResetCommand;
import com.valadir.application.command.LoginCommand;
import com.valadir.application.command.LogoutCommand;
import com.valadir.application.command.RefreshTokenCommand;
import com.valadir.application.command.RegisterCommand;
import com.valadir.application.command.ResendAccountActivationCodeCommand;
import com.valadir.application.command.VerifyPasswordResetOtpCommand;
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
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.dto.request.ActivateAccountRequest;
import com.valadir.web.dto.request.CompletePasswordResetRequest;
import com.valadir.web.dto.request.InitiatePasswordResetRequest;
import com.valadir.web.dto.request.LoginRequest;
import com.valadir.web.dto.request.LogoutRequest;
import com.valadir.web.dto.request.RefreshRequest;
import com.valadir.web.dto.request.RegisterRequest;
import com.valadir.web.dto.request.ResendAccountActivationCodeRequest;
import com.valadir.web.dto.request.VerifyPasswordResetOtpRequest;
import com.valadir.web.dto.response.AuthResponse;
import com.valadir.web.dto.response.PasswordResetOtpVerificationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@RestController
@RequestMapping(ApiRoutes.Auth.BASE)
class AuthController {

    private final RegisterUseCase registerUseCase;
    private final ActivateAccountUseCase activateAccountUseCase;
    private final ResendAccountActivationCodeUseCase resendAccountActivationCodeUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final InitiatePasswordResetUseCase initiatePasswordResetUseCase;
    private final VerifyPasswordResetOtpUseCase verifyPasswordResetOtpUseCase;
    private final CompletePasswordResetUseCase completePasswordResetUseCase;

    AuthController(
        RegisterUseCase registerUseCase,
        ActivateAccountUseCase activateAccountUseCase,
        ResendAccountActivationCodeUseCase resendAccountActivationCodeUseCase,
        LoginUseCase loginUseCase,
        RefreshTokenUseCase refreshTokenUseCase,
        LogoutUseCase logoutUseCase,
        InitiatePasswordResetUseCase initiatePasswordResetUseCase,
        VerifyPasswordResetOtpUseCase verifyPasswordResetOtpUseCase,
        CompletePasswordResetUseCase completePasswordResetUseCase
    ) {

        this.registerUseCase = registerUseCase;
        this.activateAccountUseCase = activateAccountUseCase;
        this.resendAccountActivationCodeUseCase = resendAccountActivationCodeUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.initiatePasswordResetUseCase = initiatePasswordResetUseCase;
        this.verifyPasswordResetOtpUseCase = verifyPasswordResetOtpUseCase;
        this.completePasswordResetUseCase = completePasswordResetUseCase;
    }

    @PostMapping(ApiRoutes.Auth.REGISTER)
    @ResponseStatus(HttpStatus.CREATED)
    void register(@Valid @RequestBody RegisterRequest request) {

        registerUseCase.register(new RegisterCommand(
            request.email(),
            request.password(),
            request.fullName(),
            request.givenName()
        ));
    }

    @PostMapping(ApiRoutes.Auth.AccountActivation.ACTIVATE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void activateAccount(@Valid @RequestBody ActivateAccountRequest request) {

        activateAccountUseCase.activate(new ActivateAccountCommand(request.email(), request.code()));
    }

    @PostMapping(ApiRoutes.Auth.AccountActivation.RESEND)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resendAccountActivationCode(@Valid @RequestBody ResendAccountActivationCodeRequest request) {

        resendAccountActivationCodeUseCase.resend(new ResendAccountActivationCodeCommand(request.email()));
    }

    @PostMapping(ApiRoutes.Auth.LOGIN)
    AuthResponse login(@Valid @RequestBody LoginRequest request) {

        AuthTokenResult result = loginUseCase.login(new LoginCommand(request.email(), request.password()));

        return new AuthResponse(result.accessToken(), result.refreshToken());
    }

    @PostMapping(ApiRoutes.Auth.REFRESH)
    AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {

        AuthTokenResult result = refreshTokenUseCase.refresh(new RefreshTokenCommand(request.refreshToken()));

        return new AuthResponse(result.accessToken(), result.refreshToken());
    }

    @PostMapping(ApiRoutes.Auth.LOGOUT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody LogoutRequest request, @AuthenticationPrincipal Jwt jwt) {

        Duration remainingTtl = Duration.between(Instant.now(), Objects.requireNonNull(jwt.getExpiresAt()));

        logoutUseCase.logout(new LogoutCommand(jwt.getId(), remainingTtl, request.refreshToken(), jwt.getSubject()));
    }

    @PostMapping(ApiRoutes.Auth.PasswordReset.INITIATE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void initiatePasswordReset(@Valid @RequestBody InitiatePasswordResetRequest request) {

        initiatePasswordResetUseCase.initiate(new InitiatePasswordResetCommand(request.email()));
    }

    @PostMapping(ApiRoutes.Auth.PasswordReset.VERIFY)
    PasswordResetOtpVerificationResponse verifyPasswordResetOtp(@Valid @RequestBody VerifyPasswordResetOtpRequest request) {

        PasswordResetOtpVerificationResult result = verifyPasswordResetOtpUseCase.verify(new VerifyPasswordResetOtpCommand(request.email(), request.code()));

        return new PasswordResetOtpVerificationResponse(result.verificationToken());
    }

    @PostMapping(ApiRoutes.Auth.PasswordReset.COMPLETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void completePasswordReset(@Valid @RequestBody CompletePasswordResetRequest request) {

        completePasswordResetUseCase.complete(new CompletePasswordResetCommand(request.verificationToken(), request.newPassword()));
    }
}
