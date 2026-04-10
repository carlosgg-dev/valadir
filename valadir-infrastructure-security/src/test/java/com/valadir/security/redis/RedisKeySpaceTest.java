package com.valadir.security.redis;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RedisKeySpaceTest {

    private static final String JTI = "abc-123";
    private static final String TOKEN = "refresh-token-xyz";
    private static final String PATH_KEY = "api_auth_login";
    private static final String IP = "192.168.1.1";
    private static final String EMAIL = "user@example.com";
    private static final UUID ACCOUNT_UUID = UUID.randomUUID();

    @Test
    void forBlacklist_returnsExpectedKey() {

        assertThat(RedisKeySpace.forBlacklist(JTI)).isEqualTo("auth:blacklist:" + JTI);
    }

    @Test
    void forRefreshToken_returnsExpectedKey() {

        assertThat(RedisKeySpace.forRefreshToken(TOKEN)).isEqualTo("auth:refresh_token:" + TOKEN);
    }

    @Test
    void forUserTokens_returnsExpectedKey() {

        assertThat(RedisKeySpace.forUserTokens(ACCOUNT_UUID.toString()))
            .isEqualTo("auth:user_tokens:" + ACCOUNT_UUID);
    }

    @Test
    void forRateLimitIp_returnsExpectedKey() {

        assertThat(RedisKeySpace.forRateLimitIp(PATH_KEY, IP))
            .isEqualTo("rate_limit:ip:" + PATH_KEY + ":" + IP);
    }

    @Test
    void forRateLimitEmail_returnsExpectedKey() {

        assertThat(RedisKeySpace.forRateLimitEmail(PATH_KEY, EMAIL))
            .isEqualTo("rate_limit:email:" + PATH_KEY + ":" + EMAIL);
    }

    @Test
    void forRateLimitUser_returnsExpectedKey() {

        final var accountId = ACCOUNT_UUID.toString();

        assertThat(RedisKeySpace.forRateLimitUser(accountId))
            .isEqualTo("rate_limit:user:" + accountId);
    }
}
