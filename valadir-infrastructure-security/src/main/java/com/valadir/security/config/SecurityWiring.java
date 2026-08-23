package com.valadir.security.config;

import com.valadir.application.port.out.AccessTokenRevocation;
import com.valadir.application.port.out.AccountTokensInvalidator;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.LogoutTokensInvalidator;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.application.port.out.PasswordResetVerificationTokenRepository;
import com.valadir.application.port.out.RefreshTokenRepository;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.security.adapter.AccessTokenRevocationRedisAdapter;
import com.valadir.security.adapter.AccountTokensInvalidatorRedisAdapter;
import com.valadir.security.adapter.AuthTokenIssuerJwtAdapter;
import com.valadir.security.adapter.LogoutTokensInvalidatorRedisAdapter;
import com.valadir.security.adapter.OtpRepositoryRedisAdapter;
import com.valadir.security.adapter.PasswordHasherArgon2Adapter;
import com.valadir.security.adapter.PasswordResetVerificationTokenRepositoryRedisAdapter;
import com.valadir.security.adapter.RateLimiterRedisAdapter;
import com.valadir.security.adapter.RefreshTokenRepositoryRedisAdapter;
import com.valadir.security.redis.RedisCircuitGuard;
import com.valadir.security.redis.RedisKeySpace;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

@Configuration
class SecurityWiring {

    @Bean
    RedisCircuitGuard redisCircuitGuard(CircuitBreakerRegistry circuitBreakerRegistry) {

        return new RedisCircuitGuard(circuitBreakerRegistry.circuitBreaker("redis"));
    }

    @Bean
    AuthTokenIssuer authTokenIssuer(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {

        return new AuthTokenIssuerJwtAdapter(jwtEncoder, jwtProperties);
    }

    @Bean
    RefreshTokenRepository refreshTokenRepository(
        RedisTemplate<String, String> redisTemplate,
        RedisCircuitGuard redisCircuitGuard,
        JwtProperties jwtProperties
    ) {

        return new RefreshTokenRepositoryRedisAdapter(redisTemplate, redisCircuitGuard, jwtProperties);
    }

    @Bean
    AccessTokenRevocation accessTokenRevocation(RedisTemplate<String, String> redisTemplate, RedisCircuitGuard redisCircuitGuard) {

        return new AccessTokenRevocationRedisAdapter(redisTemplate, redisCircuitGuard);
    }

    @Bean
    AccountTokensInvalidator accountTokensInvalidator(
        RedisTemplate<String, String> redisTemplate,
        RedisCircuitGuard redisCircuitGuard,
        JwtProperties jwtProperties
    ) {

        return new AccountTokensInvalidatorRedisAdapter(redisTemplate, redisCircuitGuard, jwtProperties);
    }

    @Bean
    LogoutTokensInvalidator logoutTokensInvalidator(RedisTemplate<String, String> redisTemplate, RedisCircuitGuard redisCircuitGuard) {

        return new LogoutTokensInvalidatorRedisAdapter(redisTemplate, redisCircuitGuard);
    }

    @Bean
    PasswordHasher passwordHasher(Argon2PasswordEncoder argon2PasswordEncoder) {

        return new PasswordHasherArgon2Adapter(argon2PasswordEncoder);
    }

    @Bean
    RateLimiter rateLimiter(RedisTemplate<String, String> redisTemplate, RedisCircuitGuard redisCircuitGuard) {

        return new RateLimiterRedisAdapter(redisTemplate, redisCircuitGuard);
    }

    @Bean
    OtpRepository accountActivationOtpRepository(RedisTemplate<String, String> redisTemplate, RedisCircuitGuard redisCircuitGuard) {

        return new OtpRepositoryRedisAdapter(redisTemplate, redisCircuitGuard, RedisKeySpace::forAccountActivationOtp);
    }

    @Bean
    OtpRepository passwordResetOtpRepository(RedisTemplate<String, String> redisTemplate, RedisCircuitGuard redisCircuitGuard) {

        return new OtpRepositoryRedisAdapter(redisTemplate, redisCircuitGuard, RedisKeySpace::forPasswordResetOtp);
    }

    @Bean
    PasswordResetVerificationTokenRepository passwordResetVerificationTokenRepository(
        RedisTemplate<String, String> redisTemplate,
        RedisCircuitGuard redisCircuitGuard
    ) {

        return new PasswordResetVerificationTokenRepositoryRedisAdapter(redisTemplate, redisCircuitGuard);
    }
}
