package com.valadir.web.adapter;

import com.valadir.application.command.LoginCommand;
import com.valadir.application.command.LogoutCommand;
import com.valadir.application.command.RefreshTokenCommand;
import com.valadir.application.command.RegisterCommand;
import com.valadir.application.command.ResendVerificationCommand;
import com.valadir.application.command.VerifyEmailCommand;
import com.valadir.application.port.in.LoginUseCase;
import com.valadir.application.port.in.LogoutUseCase;
import com.valadir.application.port.in.RefreshTokenUseCase;
import com.valadir.application.port.in.RegisterUseCase;
import com.valadir.application.port.in.ResendVerificationUseCase;
import com.valadir.application.port.in.VerifyEmailUseCase;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.dto.request.LoginRequest;
import com.valadir.web.dto.request.LogoutRequest;
import com.valadir.web.dto.request.RefreshRequest;
import com.valadir.web.dto.request.RegisterRequest;
import com.valadir.web.dto.request.ResendVerificationRequest;
import com.valadir.web.dto.request.VerifyEmailRequest;
import com.valadir.web.dto.response.AuthResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Objects;

@RestController
@RequestMapping(ApiRoutes.Auth.BASE)
class AuthController {

    private final RegisterUseCase registerUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ResendVerificationUseCase resendVerificationUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    AuthController(
        RegisterUseCase registerUseCase,
        VerifyEmailUseCase verifyEmailUseCase,
        ResendVerificationUseCase resendVerificationUseCase,
        LoginUseCase loginUseCase,
        RefreshTokenUseCase refreshTokenUseCase,
        LogoutUseCase logoutUseCase
    ) {

        this.registerUseCase = registerUseCase;
        this.verifyEmailUseCase = verifyEmailUseCase;
        this.resendVerificationUseCase = resendVerificationUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
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

    @PostMapping(ApiRoutes.Auth.VERIFY_EMAIL)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {

        verifyEmailUseCase.verify(new VerifyEmailCommand(request.email(), request.code()));
    }

    @PostMapping(ApiRoutes.Auth.RESEND_VERIFICATION)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resendVerification(@Valid @RequestBody ResendVerificationRequest request) {

        resendVerificationUseCase.resend(new ResendVerificationCommand(request.email()));
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

        long remainingTtl = Objects.requireNonNull(jwt.getExpiresAt()).getEpochSecond() - Instant.now().getEpochSecond();

        logoutUseCase.logout(new LogoutCommand(jwt.getId(), remainingTtl, request.refreshToken(), jwt.getSubject()));
    }
}
