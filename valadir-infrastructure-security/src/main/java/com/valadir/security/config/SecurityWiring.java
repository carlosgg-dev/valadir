package com.valadir.security.config;

import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.LogoutTokensInvalidator;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.application.port.out.PasswordResetVerificationTokenRepository;
import com.valadir.application.port.out.RefreshTokenRepository;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.security.adapter.AccessTokenBlacklistRedisAdapter;
import com.valadir.security.adapter.Argon2PasswordHasher;
import com.valadir.security.adapter.AuthTokenIssuerJwtAdapter;
import com.valadir.security.adapter.LogoutTokensInvalidatorRedisAdapter;
import com.valadir.security.adapter.OtpRepositoryRedisAdapter;
import com.valadir.security.adapter.PasswordResetVerificationTokenRepositoryRedisAdapter;
import com.valadir.security.adapter.RedisRateLimiterAdapter;
import com.valadir.security.adapter.RefreshTokenRepositoryRedisAdapter;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

@Configuration
class SecurityWiring {

    @Bean
    AuthTokenIssuer authTokenIssuer(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {

        return new AuthTokenIssuerJwtAdapter(jwtEncoder, jwtProperties);
    }

    @Bean
    RefreshTokenRepository refreshTokenRepository(RedisTemplate<String, String> redisTemplate, JwtProperties jwtProperties) {

        return new RefreshTokenRepositoryRedisAdapter(redisTemplate, jwtProperties);
    }

    @Bean
    AccessTokenBlacklist accessTokenBlacklist(RedisTemplate<String, String> redisTemplate) {

        return new AccessTokenBlacklistRedisAdapter(redisTemplate);
    }

    @Bean
    LogoutTokensInvalidator logoutTokensInvalidator(RedisTemplate<String, String> redisTemplate) {

        return new LogoutTokensInvalidatorRedisAdapter(redisTemplate);
    }

    @Bean
    PasswordHasher passwordHasher(Argon2PasswordEncoder argon2PasswordEncoder) {

        return new Argon2PasswordHasher(argon2PasswordEncoder);
    }

    @Bean
    RateLimiter rateLimiter(RedisTemplate<String, String> redisTemplate) {

        return new RedisRateLimiterAdapter(redisTemplate);
    }

    @Bean
    OtpRepository accountActivationOtpRepository(RedisTemplate<String, String> redisTemplate) {

        return new OtpRepositoryRedisAdapter(redisTemplate, RedisKeySpace::forAccountActivationOtp);
    }

    @Bean
    OtpRepository passwordResetOtpRepository(RedisTemplate<String, String> redisTemplate) {

        return new OtpRepositoryRedisAdapter(redisTemplate, RedisKeySpace::forPasswordResetOtp);
    }

    @Bean
    PasswordResetVerificationTokenRepository passwordResetVerificationTokenRepository(RedisTemplate<String, String> redisTemplate) {

        return new PasswordResetVerificationTokenRepositoryRedisAdapter(redisTemplate);
    }
}
