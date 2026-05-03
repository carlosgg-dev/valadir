package com.valadir.config;

import com.valadir.application.config.VerificationConfig;
import com.valadir.application.port.in.LoginUseCase;
import com.valadir.application.port.in.LogoutUseCase;
import com.valadir.application.port.in.PurgeExpiredPendingAccountsUseCase;
import com.valadir.application.port.in.RefreshTokenUseCase;
import com.valadir.application.port.in.RegisterUseCase;
import com.valadir.application.port.in.ResendVerificationUseCase;
import com.valadir.application.port.in.VerifyEmailUseCase;
import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.EmailVerificationPort;
import com.valadir.application.port.out.ExpiredPendingAccountCleaner;
import com.valadir.application.port.out.LoginAttemptStore;
import com.valadir.application.port.out.LogoutTokensInvalidator;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpStore;
import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.application.service.LoginService;
import com.valadir.application.service.LogoutService;
import com.valadir.application.service.OtpVerificationSender;
import com.valadir.application.service.OtpVerificationSenderService;
import com.valadir.application.service.PurgeExpiredPendingAccountsService;
import com.valadir.application.service.RefreshTokenService;
import com.valadir.application.service.RegisterService;
import com.valadir.application.service.ResendVerificationService;
import com.valadir.application.service.VerifyEmailService;
import com.valadir.domain.policy.LoginLockoutPolicy;
import com.valadir.domain.policy.LoginLockoutThreshold;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.domain.service.PasswordSecurityService;
import com.valadir.security.adapter.Argon2OtpHasher;
import com.valadir.security.adapter.BlacklistAwareJwtDecoder;
import com.valadir.security.adapter.LoginAttemptRedisAdapter;
import com.valadir.security.adapter.OtpRedisAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Configuration
@EnableConfigurationProperties(LoginLockoutProperties.class)
class ApplicationWiring {

    @Bean
    PasswordSecurityService passwordSecurityService() {

        return new PasswordSecurityService();
    }

    @Bean
    OtpStore otpStore(RedisTemplate<String, String> redisTemplate) {

        return new OtpRedisAdapter(redisTemplate);
    }

    @Bean
    OtpHasher otpHasher(Argon2PasswordEncoder argon2PasswordEncoder) {

        return new Argon2OtpHasher(argon2PasswordEncoder);
    }

    @Bean
    Clock systemClock() {

        return Clock.systemUTC();
    }

    @Bean
    VerificationConfig verificationConfig(
        @Value("${auth.otp.ttl}") Duration otpTtl,
        @Value("${scheduler.pending-account.grace-period}") Duration accountGracePeriod
    ) {

        return new VerificationConfig(otpTtl, accountGracePeriod);
    }

    @Bean
    PurgeExpiredPendingAccountsUseCase purgeExpiredPendingAccountsUseCase(
        ExpiredPendingAccountCleaner expiredPendingAccountCleaner,
        VerificationConfig verificationConfig,
        Clock clock
    ) {

        return new PurgeExpiredPendingAccountsService(expiredPendingAccountCleaner, verificationConfig, clock);
    }

    @Bean
    OtpVerificationSender otpVerificationSender(
        EmailVerificationPort emailVerificationPort,
        OtpStore otpStore,
        OtpHasher otpHasher,
        VerificationConfig verificationConfig
    ) {

        return new OtpVerificationSenderService(emailVerificationPort, otpStore, otpHasher, verificationConfig);
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
    VerifyEmailUseCase verifyEmailUseCase(AccountRepository accountRepository, OtpStore otpStore, OtpHasher otpHasher) {

        return new VerifyEmailService(accountRepository, otpStore, otpHasher);
    }

    @Bean
    ResendVerificationUseCase resendVerificationUseCase(AccountRepository accountRepository, OtpVerificationSender otpVerificationSender) {

        return new ResendVerificationService(accountRepository, otpVerificationSender);
    }

    @Bean
    LoginLockoutPolicy loginLockoutPolicy(LoginLockoutProperties properties) {

        List<LoginLockoutThreshold> thresholds = properties.thresholds().stream()
            .map(threshold -> new LoginLockoutThreshold(threshold.minFailures(), threshold.lockout()))
            .toList();

        return new LoginLockoutPolicy(properties.window(), thresholds);
    }

    @Bean
    LoginAttemptStore loginAttemptStore(RedisTemplate<String, String> redisTemplate, LoginLockoutPolicy loginLockoutPolicy) {

        return new LoginAttemptRedisAdapter(redisTemplate, loginLockoutPolicy);
    }

    @Bean
    LoginUseCase loginUseCase(
        AccountRepository accountRepository,
        PasswordHasher passwordHasher,
        AuthTokenIssuer authTokenIssuer,
        RefreshTokenStore refreshTokenStore,
        LoginAttemptStore loginAttemptStore
    ) {

        return new LoginService(accountRepository, passwordHasher, authTokenIssuer, refreshTokenStore, loginAttemptStore);
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
