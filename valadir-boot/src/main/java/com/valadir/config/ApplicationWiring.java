package com.valadir.config;

import com.valadir.application.config.AccountActivationConfig;
import com.valadir.application.config.PendingActivationAccountPurgeConfig;
import com.valadir.application.port.in.ActivateAccountUseCase;
import com.valadir.application.port.in.LoginUseCase;
import com.valadir.application.port.in.LogoutUseCase;
import com.valadir.application.port.in.PurgeExpiredPendingActivationAccountsUseCase;
import com.valadir.application.port.in.RefreshTokenUseCase;
import com.valadir.application.port.in.RegisterUseCase;
import com.valadir.application.port.in.ResendAccountActivationCodeUseCase;
import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.application.port.out.AccountActivationPort;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.ExpiredPendingActivationAccountCleaner;
import com.valadir.application.port.out.LoginAttemptStore;
import com.valadir.application.port.out.LogoutTokensInvalidator;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpStore;
import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.application.service.AccountActivationOtpSender;
import com.valadir.application.service.AccountActivationOtpSenderService;
import com.valadir.application.service.ActivateAccountService;
import com.valadir.application.service.LoginService;
import com.valadir.application.service.LogoutService;
import com.valadir.application.service.PurgeExpiredPendingActivationAccountsService;
import com.valadir.application.service.RefreshTokenService;
import com.valadir.application.service.RegisterService;
import com.valadir.application.service.ResendAccountActivationCodeService;
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
    AccountActivationConfig accountActivationConfig(@Value("${auth.account-activation.otp.ttl}") Duration otpTtl) {

        return new AccountActivationConfig(otpTtl);
    }

    @Bean
    PendingActivationAccountPurgeConfig pendingAccountPurgeConfig(
        @Value("${scheduler.pending-activation-account.grace-period}") Duration accountGracePeriod
    ) {

        return new PendingActivationAccountPurgeConfig(accountGracePeriod);
    }

    @Bean
    PurgeExpiredPendingActivationAccountsUseCase purgeExpiredPendingAccountsUseCase(
        ExpiredPendingActivationAccountCleaner expiredPendingActivationAccountCleaner,
        PendingActivationAccountPurgeConfig pendingActivationAccountPurgeConfig,
        Clock clock
    ) {

        return new PurgeExpiredPendingActivationAccountsService(expiredPendingActivationAccountCleaner, pendingActivationAccountPurgeConfig, clock);
    }

    @Bean
    AccountActivationOtpSender accountActivationOtpSender(
        AccountActivationPort accountActivationPort,
        OtpStore otpStore,
        OtpHasher otpHasher,
        AccountActivationConfig accountActivationConfig
    ) {

        return new AccountActivationOtpSenderService(accountActivationPort, otpStore, otpHasher, accountActivationConfig);
    }

    @Bean
    RegisterUseCase registerUseCase(
        AccountRepository accountRepository,
        PasswordHasher passwordHasher,
        PasswordSecurityService passwordSecurityService,
        RegisterPersistence registerPersistence,
        AccountActivationOtpSender accountActivationOtpSender
    ) {

        return new RegisterService(
            accountRepository,
            passwordHasher,
            passwordSecurityService,
            registerPersistence,
            accountActivationOtpSender
        );
    }

    @Bean
    ActivateAccountUseCase activateAccountUseCase(AccountRepository accountRepository, OtpStore otpStore, OtpHasher otpHasher) {

        return new ActivateAccountService(accountRepository, otpStore, otpHasher);
    }

    @Bean
    ResendAccountActivationCodeUseCase resendAccountActivationCodeUseCase(
        AccountRepository accountRepository,
        AccountActivationOtpSender accountActivationOtpSender
    ) {

        return new ResendAccountActivationCodeService(accountRepository, accountActivationOtpSender);
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
