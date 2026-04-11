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
        final AccountRepository accountRepository,
        final PasswordHasher passwordHasher,
        final PasswordSecurityService passwordSecurityService,
        final RegisterPersistence registerPersistence,
        final AuthTokenIssuer authTokenIssuer,
        final RefreshTokenStore refreshTokenStore
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
        final AccountRepository accountRepository,
        final PasswordHasher passwordHasher,
        final AuthTokenIssuer authTokenIssuer,
        final RefreshTokenStore refreshTokenStore
    ) {

        return new LoginService(accountRepository, passwordHasher, authTokenIssuer, refreshTokenStore);
    }

    @Bean
    RefreshTokenUseCase refreshTokenUseCase(
        final RefreshTokenStore refreshTokenStore,
        final AccountRepository accountRepository,
        final AuthTokenIssuer authTokenIssuer
    ) {

        return new RefreshTokenService(refreshTokenStore, accountRepository, authTokenIssuer);
    }

    @Bean
    LogoutUseCase logoutUseCase(final LogoutTokensInvalidator logoutTokensInvalidator) {

        return new LogoutService(logoutTokensInvalidator);
    }

    @Bean
    JwtDecoder jwtDecoder(
        @Qualifier("nimbusJwtDecoder") final JwtDecoder delegate,
        final AccessTokenBlacklist accessTokenBlacklist
    ) {

        return new BlacklistAwareJwtDecoder(delegate, accessTokenBlacklist);
    }
}
