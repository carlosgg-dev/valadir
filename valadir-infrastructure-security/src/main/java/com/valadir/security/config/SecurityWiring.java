package com.valadir.security.config;

import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.LogoutTokensInvalidator;
import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.common.ratelimit.RateLimiter;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.security.adapter.AccessTokenBlacklistRedisAdapter;
import com.valadir.security.adapter.Argon2PasswordHasher;
import com.valadir.security.adapter.AuthTokenIssuerJwtAdapter;
import com.valadir.security.adapter.LogoutTokensInvalidatorRedisAdapter;
import com.valadir.security.adapter.RedisRateLimiterAdapter;
import com.valadir.security.adapter.RefreshTokenStoreRedisAdapter;
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
    RefreshTokenStore refreshTokenStore(RedisTemplate<String, String> redisTemplate, JwtProperties jwtProperties) {

        return new RefreshTokenStoreRedisAdapter(redisTemplate, jwtProperties);
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
}
