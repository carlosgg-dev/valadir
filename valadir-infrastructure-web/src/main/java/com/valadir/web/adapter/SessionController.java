package com.valadir.web.adapter;

import com.valadir.application.command.LoginCommand;
import com.valadir.application.command.LogoutCommand;
import com.valadir.application.command.RefreshTokenCommand;
import com.valadir.application.port.in.LoginUseCase;
import com.valadir.application.port.in.LogoutUseCase;
import com.valadir.application.port.in.RefreshTokenUseCase;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.dto.request.LoginRequest;
import com.valadir.web.dto.request.LogoutRequest;
import com.valadir.web.dto.request.RefreshRequest;
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

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@RestController
@RequestMapping(ApiRoutes.Auth.BASE)
class SessionController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    SessionController(
        LoginUseCase loginUseCase,
        RefreshTokenUseCase refreshTokenUseCase,
        LogoutUseCase logoutUseCase
    ) {

        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
    }

    @PostMapping(ApiRoutes.Auth.Session.LOGIN)
    AuthResponse login(@Valid @RequestBody LoginRequest request) {

        AuthTokenResult result = loginUseCase.login(new LoginCommand(request.email(), request.password()));

        return new AuthResponse(result.accessToken(), result.refreshToken());
    }

    @PostMapping(ApiRoutes.Auth.Session.REFRESH)
    AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {

        AuthTokenResult result = refreshTokenUseCase.refresh(new RefreshTokenCommand(request.refreshToken()));

        return new AuthResponse(result.accessToken(), result.refreshToken());
    }

    @PostMapping(ApiRoutes.Auth.Session.LOGOUT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody LogoutRequest request, @AuthenticationPrincipal Jwt jwt) {

        Duration remainingTtl = Duration.between(Instant.now(), Objects.requireNonNull(jwt.getExpiresAt()));

        logoutUseCase.logout(new LogoutCommand(jwt.getId(), remainingTtl, request.refreshToken(), jwt.getSubject()));
    }
}
