package com.valadir.config;

import com.valadir.application.config.AccountActivationConfig;
import com.valadir.application.config.PasswordResetConfig;
import com.valadir.application.config.PendingActivationAccountPurgeConfig;
import com.valadir.application.port.in.ActivateAccountUseCase;
import com.valadir.application.port.in.CompletePasswordResetUseCase;
import com.valadir.application.port.in.InitiatePasswordResetUseCase;
import com.valadir.application.port.in.LoginUseCase;
import com.valadir.application.port.in.LogoutUseCase;
import com.valadir.application.port.in.PurgeExpiredPendingActivationAccountsUseCase;
import com.valadir.application.port.in.RefreshTokenUseCase;
import com.valadir.application.port.in.RegisterUseCase;
import com.valadir.application.port.in.ResendAccountActivationCodeUseCase;
import com.valadir.application.port.in.VerifyPasswordResetOtpUseCase;
import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.application.port.out.AccountActivationNotifier;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.ExpiredPendingActivationAccountCleaner;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.application.port.out.LogoutTokensInvalidator;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.application.port.out.PasswordResetNotifier;
import com.valadir.application.port.out.PasswordResetOtpRepository;
import com.valadir.application.port.out.PasswordResetVerificationTokenRepository;
import com.valadir.application.port.out.RefreshTokenRepository;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.application.port.out.UserRepository;
import com.valadir.application.service.AccountActivationOtpSender;
import com.valadir.application.service.AccountActivationOtpSenderService;
import com.valadir.application.service.ActivateAccountService;
import com.valadir.application.service.CompletePasswordResetService;
import com.valadir.application.service.InitiatePasswordResetService;
import com.valadir.application.service.LoginService;
import com.valadir.application.service.LogoutService;
import com.valadir.application.service.PurgeExpiredPendingActivationAccountsService;
import com.valadir.application.service.RefreshTokenService;
import com.valadir.application.service.RegisterService;
import com.valadir.application.service.ResendAccountActivationCodeService;
import com.valadir.application.service.VerifyPasswordResetOtpService;

import com.valadir.domain.policy.LoginLockoutPolicy;
import com.valadir.domain.policy.LoginLockoutThreshold;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.domain.service.PasswordSecurityService;
import com.valadir.security.adapter.BlacklistAwareJwtDecoder;
import com.valadir.security.adapter.LoginAttemptRepositoryRedisAdapter;
import com.valadir.security.adapter.OtpHasherArgon2Adapter;
import com.valadir.security.adapter.OtpRepositoryRedisAdapter;
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
    OtpRepository otpRepository(RedisTemplate<String, String> redisTemplate) {

        return new OtpRepositoryRedisAdapter(redisTemplate);
    }

    @Bean
    OtpHasher otpHasher(Argon2PasswordEncoder argon2PasswordEncoder) {

        return new OtpHasherArgon2Adapter(argon2PasswordEncoder);
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
    PendingActivationAccountPurgeConfig pendingActivationAccountPurgeConfig(
        @Value("${scheduler.pending-activation-account.grace-period}") Duration accountGracePeriod
    ) {

        return new PendingActivationAccountPurgeConfig(accountGracePeriod);
    }

    @Bean
    PurgeExpiredPendingActivationAccountsUseCase purgeExpiredPendingActivationAccountsUseCase(
        ExpiredPendingActivationAccountCleaner expiredPendingActivationAccountCleaner,
        PendingActivationAccountPurgeConfig pendingActivationAccountPurgeConfig,
        Clock clock
    ) {

        return new PurgeExpiredPendingActivationAccountsService(expiredPendingActivationAccountCleaner, pendingActivationAccountPurgeConfig, clock);
    }

    @Bean
    AccountActivationOtpSender accountActivationOtpSender(
        AccountActivationNotifier accountActivationNotifier,
        OtpRepository otpRepository,
        OtpHasher otpHasher,
        AccountActivationConfig accountActivationConfig
    ) {

        return new AccountActivationOtpSenderService(accountActivationNotifier, otpRepository, otpHasher, accountActivationConfig);
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
    ActivateAccountUseCase activateAccountUseCase(AccountRepository accountRepository, OtpRepository otpRepository, OtpHasher otpHasher) {

        return new ActivateAccountService(accountRepository, otpRepository, otpHasher);
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
    LoginAttemptRepository loginAttemptRepository(RedisTemplate<String, String> redisTemplate, LoginLockoutPolicy loginLockoutPolicy) {

        return new LoginAttemptRepositoryRedisAdapter(redisTemplate, loginLockoutPolicy);
    }

    @Bean
    LoginUseCase loginUseCase(
        AccountRepository accountRepository,
        PasswordHasher passwordHasher,
        AuthTokenIssuer authTokenIssuer,
        RefreshTokenRepository refreshTokenRepository,
        LoginAttemptRepository loginAttemptRepository
    ) {

        return new LoginService(accountRepository, passwordHasher, authTokenIssuer, refreshTokenRepository, loginAttemptRepository);
    }

    @Bean
    RefreshTokenUseCase refreshTokenUseCase(
        RefreshTokenRepository refreshTokenRepository,
        AccountRepository accountRepository,
        AuthTokenIssuer authTokenIssuer
    ) {

        return new RefreshTokenService(refreshTokenRepository, accountRepository, authTokenIssuer);
    }

    @Bean
    LogoutUseCase logoutUseCase(LogoutTokensInvalidator logoutTokensInvalidator) {

        return new LogoutService(logoutTokensInvalidator);
    }

    @Bean
    PasswordResetConfig passwordResetConfig(
        @Value("${auth.password-reset.otp.ttl}") Duration otpTtl,
        @Value("${auth.password-reset.verification-token.ttl}") Duration verificationTokenTtl
    ) {

        return new PasswordResetConfig(otpTtl, verificationTokenTtl);
    }

    @Bean
    InitiatePasswordResetUseCase initiatePasswordResetUseCase(
        AccountRepository accountRepository,
        PasswordResetOtpRepository passwordResetOtpRepository,
        OtpHasher otpHasher,
        PasswordResetNotifier passwordResetNotifier,
        PasswordResetConfig passwordResetConfig
    ) {

        return new InitiatePasswordResetService(accountRepository, passwordResetOtpRepository, otpHasher, passwordResetNotifier, passwordResetConfig);
    }

    @Bean
    VerifyPasswordResetOtpUseCase verifyPasswordResetOtpUseCase(
        AccountRepository accountRepository,
        PasswordResetOtpRepository passwordResetOtpRepository,
        OtpHasher otpHasher,
        PasswordResetVerificationTokenRepository passwordResetVerificationTokenRepository,
        PasswordResetConfig passwordResetConfig
    ) {

        return new VerifyPasswordResetOtpService(accountRepository, passwordResetOtpRepository, otpHasher, passwordResetVerificationTokenRepository, passwordResetConfig);
    }

    @Bean
    CompletePasswordResetUseCase completePasswordResetUseCase(
        PasswordResetVerificationTokenRepository passwordResetVerificationTokenRepository,
        AccountRepository accountRepository,
        UserRepository userRepository,
        PasswordHasher passwordHasher,
        PasswordSecurityService passwordSecurityService,
        RefreshTokenRepository refreshTokenRepository
    ) {

        return new CompletePasswordResetService(
            passwordResetVerificationTokenRepository,
            accountRepository,
            userRepository,
            passwordHasher,
            passwordSecurityService,
            refreshTokenRepository
        );
    }

    @Bean
    JwtDecoder jwtDecoder(@Qualifier("nimbusJwtDecoder") JwtDecoder delegate, AccessTokenBlacklist accessTokenBlacklist) {

        return new BlacklistAwareJwtDecoder(delegate, accessTokenBlacklist);
    }
}
