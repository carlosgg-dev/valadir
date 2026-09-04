package com.valadir.config;

import com.valadir.application.config.AccountActivationConfig;
import com.valadir.application.config.PasswordResetConfig;
import com.valadir.application.config.PendingActivationAccountPurgeConfig;
import com.valadir.application.port.in.ActivateAccountUseCase;
import com.valadir.application.port.in.CompletePasswordResetUseCase;
import com.valadir.application.port.in.InitiatePasswordResetUseCase;
import com.valadir.application.port.in.LoginUseCase;
import com.valadir.application.port.in.LogoutAllUseCase;
import com.valadir.application.port.in.LogoutUseCase;
import com.valadir.application.port.in.PurgeExpiredPendingActivationAccountsUseCase;
import com.valadir.application.port.in.RefreshTokenUseCase;
import com.valadir.application.port.in.RegisterUseCase;
import com.valadir.application.port.in.ResendAccountActivationCodeUseCase;
import com.valadir.application.port.in.VerifyPasswordResetOtpUseCase;
import com.valadir.application.port.out.AccessTokenRevocation;
import com.valadir.application.port.out.AccountActivationNotifier;
import com.valadir.application.port.out.AccountLockedNotifier;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AccountTokensInvalidator;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.CaptchaVerifier;
import com.valadir.application.port.out.ExpiredPendingActivationAccountCleaner;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.application.port.out.LogoutTokensInvalidator;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.application.port.out.PasswordResetNotifier;
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
import com.valadir.application.service.LogoutAllService;
import com.valadir.application.service.LogoutService;
import com.valadir.application.service.PasswordResetOtpSender;
import com.valadir.application.service.PasswordResetOtpSenderService;
import com.valadir.application.service.PurgeExpiredPendingActivationAccountsService;
import com.valadir.application.service.RefreshTokenService;
import com.valadir.application.service.RegisterService;
import com.valadir.application.service.ResendAccountActivationCodeService;
import com.valadir.application.service.VerifyPasswordResetOtpService;
import com.valadir.domain.policy.LoginLockoutPolicy;
import com.valadir.domain.policy.LoginLockoutThreshold;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.domain.service.PasswordSecurityService;
import com.valadir.security.adapter.CaptchaVerifierTurnstileAdapter;
import com.valadir.security.adapter.LoginAttemptRepositoryRedisAdapter;
import com.valadir.security.adapter.OtpHasherArgon2Adapter;
import com.valadir.security.jwt.RevocationAwareJwtDecoder;
import com.valadir.security.redis.RedisCircuitGuard;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Configuration
@EnableConfigurationProperties({LoginLockoutProperties.class, CaptchaProperties.class})
class ApplicationWiring {

    @Bean
    PasswordSecurityService passwordSecurityService() {

        return new PasswordSecurityService();
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
        OtpRepository accountActivationOtpRepository,
        OtpHasher otpHasher,
        AccountActivationConfig accountActivationConfig
    ) {

        return new AccountActivationOtpSenderService(
            accountActivationNotifier,
            accountActivationOtpRepository,
            otpHasher,
            accountActivationConfig
        );
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
    ActivateAccountUseCase activateAccountUseCase(
        AccountRepository accountRepository,
        OtpRepository accountActivationOtpRepository,
        OtpHasher otpHasher
    ) {

        return new ActivateAccountService(accountRepository, accountActivationOtpRepository, otpHasher);
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

        return new LoginLockoutPolicy(properties.window(), properties.challengeThreshold(), thresholds);
    }

    @Bean
    LoginAttemptRepository loginAttemptRepository(
        RedisTemplate<String, String> redisTemplate,
        RedisCircuitGuard redisCircuitGuard,
        LoginLockoutPolicy loginLockoutPolicy
    ) {

        return new LoginAttemptRepositoryRedisAdapter(redisTemplate, redisCircuitGuard, loginLockoutPolicy);
    }

    @Bean
    RestClient captchaRestClient(RestClient.Builder builder, CaptchaProperties properties) {

        var settings = ClientHttpRequestFactorySettings.defaults()
            .withConnectTimeout(properties.connectTimeout())
            .withReadTimeout(properties.readTimeout());

        return builder
            .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
            .build();
    }

    @Bean
    CircuitBreaker captchaCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {

        return circuitBreakerRegistry.circuitBreaker("captcha");
    }

    @Bean
    CaptchaVerifier captchaVerifier(RestClient captchaRestClient, CaptchaProperties properties, CircuitBreaker captchaCircuitBreaker) {

        return new CaptchaVerifierTurnstileAdapter(
            captchaRestClient,
            properties.verifyUrl(),
            properties.secret(),
            properties.enabled(),
            captchaCircuitBreaker
        );
    }

    @Bean
    LoginUseCase loginUseCase(
        AccountRepository accountRepository,
        PasswordHasher passwordHasher,
        AuthTokenIssuer authTokenIssuer,
        RefreshTokenRepository refreshTokenRepository,
        LoginAttemptRepository loginAttemptRepository,
        CaptchaVerifier captchaVerifier,
        AccountLockedNotifier accountLockedNotifier
    ) {

        return new LoginService(
            accountRepository,
            passwordHasher,
            authTokenIssuer,
            refreshTokenRepository,
            loginAttemptRepository,
            captchaVerifier,
            accountLockedNotifier
        );
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
    LogoutAllUseCase logoutAllUseCase(AccountTokensInvalidator accountTokensInvalidator) {

        return new LogoutAllService(accountTokensInvalidator);
    }

    @Bean
    PasswordResetConfig passwordResetConfig(
        @Value("${auth.password-reset.otp.ttl}") Duration otpTtl,
        @Value("${auth.password-reset.verification-token.ttl}") Duration verificationTokenTtl
    ) {

        return new PasswordResetConfig(otpTtl, verificationTokenTtl);
    }

    @Bean
    PasswordResetOtpSender passwordResetOtpSender(
        PasswordResetNotifier passwordResetNotifier,
        OtpRepository passwordResetOtpRepository,
        OtpHasher otpHasher,
        PasswordResetConfig passwordResetConfig
    ) {

        return new PasswordResetOtpSenderService(
            passwordResetNotifier,
            passwordResetOtpRepository,
            otpHasher,
            passwordResetConfig
        );
    }

    @Bean
    InitiatePasswordResetUseCase initiatePasswordResetUseCase(
        AccountRepository accountRepository,
        OtpHasher otpHasher,
        PasswordResetOtpSender passwordResetOtpSender
    ) {

        return new InitiatePasswordResetService(accountRepository, otpHasher, passwordResetOtpSender);
    }

    @Bean
    VerifyPasswordResetOtpUseCase verifyPasswordResetOtpUseCase(
        AccountRepository accountRepository,
        OtpRepository passwordResetOtpRepository,
        OtpHasher otpHasher,
        PasswordResetVerificationTokenRepository passwordResetVerificationTokenRepository,
        PasswordResetConfig passwordResetConfig
    ) {

        return new VerifyPasswordResetOtpService(
            accountRepository,
            passwordResetOtpRepository,
            otpHasher,
            passwordResetVerificationTokenRepository,
            passwordResetConfig
        );
    }

    @Bean
    CompletePasswordResetUseCase completePasswordResetUseCase(
        PasswordResetVerificationTokenRepository passwordResetVerificationTokenRepository,
        AccountRepository accountRepository,
        UserRepository userRepository,
        PasswordHasher passwordHasher,
        PasswordSecurityService passwordSecurityService,
        AccountTokensInvalidator accountTokensInvalidator,
        LoginAttemptRepository loginAttemptRepository
    ) {

        return new CompletePasswordResetService(
            passwordResetVerificationTokenRepository,
            accountRepository,
            userRepository,
            passwordHasher,
            passwordSecurityService,
            accountTokensInvalidator,
            loginAttemptRepository
        );
    }

    @Bean
    JwtDecoder jwtDecoder(@Qualifier("nimbusJwtDecoder") JwtDecoder delegate, AccessTokenRevocation accessTokenRevocation) {

        return new RevocationAwareJwtDecoder(delegate, accessTokenRevocation);
    }
}
