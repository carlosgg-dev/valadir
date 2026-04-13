package com.valadir.config;

import com.valadir.application.port.in.LoginUseCase;
import com.valadir.application.port.in.LogoutUseCase;
import com.valadir.application.port.in.RefreshTokenUseCase;
import com.valadir.application.port.in.RegisterUseCase;
import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.LogoutTokensInvalidator;
import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.application.service.LoginService;
import com.valadir.application.service.LogoutService;
import com.valadir.application.service.RefreshTokenService;
import com.valadir.application.service.RegisterService;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.domain.service.PasswordSecurityService;
import com.valadir.security.adapter.BlacklistAwareJwtDecoder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@Configuration
class ApplicationWiring {

    @Bean
    PasswordSecurityService passwordSecurityService() {

        return new PasswordSecurityService();
    }

    @Bean
    RegisterUseCase registerUseCase(
        AccountRepository accountRepository,
        PasswordHasher passwordHasher,
        PasswordSecurityService passwordSecurityService,
        RegisterPersistence registerPersistence,
        AuthTokenIssuer authTokenIssuer,
        RefreshTokenStore refreshTokenStore
    ) {

        return new RegisterService(
            accountRepository,
            passwordHasher,
            passwordSecurityService,
            registerPersistence,
            authTokenIssuer,
            refreshTokenStore
        );
    }

    @Bean
    LoginUseCase loginUseCase(
        AccountRepository accountRepository,
        PasswordHasher passwordHasher,
        AuthTokenIssuer authTokenIssuer,
        RefreshTokenStore refreshTokenStore
    ) {

        return new LoginService(accountRepository, passwordHasher, authTokenIssuer, refreshTokenStore);
    }

    @Bean
    RefreshTokenUseCase refreshTokenUseCase(
        RefreshTokenStore refreshTokenStore,
        AccountRepository accountRepository,
        AuthTokenIssuer authTokenIssuer
    ) {

        return new RefreshTokenService(refreshTokenStore, accountRepository, authTokenIssuer);
    }

    @Bean
    LogoutUseCase logoutUseCase(LogoutTokensInvalidator logoutTokensInvalidator) {

        return new LogoutService(logoutTokensInvalidator);
    }

    @Bean
    JwtDecoder jwtDecoder(@Qualifier("nimbusJwtDecoder") JwtDecoder delegate, AccessTokenBlacklist accessTokenBlacklist) {

        return new BlacklistAwareJwtDecoder(delegate, accessTokenBlacklist);
    }
}
