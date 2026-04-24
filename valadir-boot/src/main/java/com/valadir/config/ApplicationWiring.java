package com.valadir.config;

import com.valadir.application.config.VerificationConfig;
import com.valadir.application.port.in.LoginUseCase;
import com.valadir.application.port.in.LogoutUseCase;
import com.valadir.application.port.in.RefreshTokenUseCase;
import com.valadir.application.port.in.RegisterUseCase;
import com.valadir.application.port.in.ResendVerificationUseCase;
import com.valadir.application.port.in.VerifyEmailUseCase;
import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.EmailVerificationPort;
import com.valadir.application.port.out.LogoutTokensInvalidator;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.application.service.LoginService;
import com.valadir.application.service.LogoutService;
import com.valadir.application.service.OtpVerificationSender;
import com.valadir.application.service.OtpVerificationSenderService;
import com.valadir.application.service.RefreshTokenService;
import com.valadir.application.service.RegisterService;
import com.valadir.application.service.ResendVerificationService;
import com.valadir.application.service.VerifyEmailService;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.domain.service.PasswordSecurityService;
import com.valadir.security.adapter.Argon2OtpHasher;
import com.valadir.security.adapter.BlacklistAwareJwtDecoder;
import com.valadir.security.adapter.OtpRedisAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Duration;

@Configuration
class ApplicationWiring {

    @Bean
    PasswordSecurityService passwordSecurityService() {

        return new PasswordSecurityService();
    }

    @Bean
    OtpRepository otpRepository(RedisTemplate<String, String> redisTemplate) {

        return new OtpRedisAdapter(redisTemplate);
    }

    @Bean
    OtpHasher otpHasher(Argon2PasswordEncoder argon2PasswordEncoder) {

        return new Argon2OtpHasher(argon2PasswordEncoder);
    }

    @Bean
    VerificationConfig verificationConfig(@Value("${verification.otp.ttl-seconds}") long ttlSeconds) {

        return new VerificationConfig(Duration.ofSeconds(ttlSeconds));
    }

    @Bean
    OtpVerificationSender otpVerificationSender(
        EmailVerificationPort emailVerificationPort,
        OtpRepository otpRepository,
        OtpHasher otpHasher,
        VerificationConfig verificationConfig
    ) {

        return new OtpVerificationSenderService(emailVerificationPort, otpRepository, otpHasher, verificationConfig);
    }

    @Bean
    RegisterUseCase registerUseCase(
        AccountRepository accountRepository,
        PasswordHasher passwordHasher,
        PasswordSecurityService passwordSecurityService,
        RegisterPersistence registerPersistence,
        OtpVerificationSender otpVerificationSender
    ) {

        return new RegisterService(
            accountRepository,
            passwordHasher,
            passwordSecurityService,
            registerPersistence,
            otpVerificationSender
        );
    }

    @Bean
    VerifyEmailUseCase verifyEmailUseCase(AccountRepository accountRepository, OtpRepository otpRepository, OtpHasher otpHasher) {

        return new VerifyEmailService(accountRepository, otpRepository, otpHasher);
    }

    @Bean
    ResendVerificationUseCase resendVerificationUseCase(AccountRepository accountRepository, OtpVerificationSender otpVerificationSender) {

        return new ResendVerificationService(accountRepository, otpVerificationSender);
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
