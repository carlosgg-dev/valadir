package com.valadir.security.redis;

import com.valadir.domain.model.AccountId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisKeySpaceTest {

    private static final String JTI = "abc-123";
    private static final String TOKEN = "refresh-token-xyz";
    private static final String PATH_KEY = "api_auth_login";
    private static final String IP = "192.168.1.1";
    private static final String EMAIL = "user@example.com";
    private static final String ACCOUNT_ID = AccountId.generate().value().toString();

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

        assertThat(RedisKeySpace.forUserTokens(ACCOUNT_ID))
            .isEqualTo("auth:user_tokens:" + ACCOUNT_ID);
    }

    @Test
    void forAccountActivationOtp_returnsExpectedKey() {

        assertThat(RedisKeySpace.forAccountActivationOtp(ACCOUNT_ID))
            .isEqualTo("auth:account_activation_otp:" + ACCOUNT_ID);
    }

    @Test
    void forPasswordResetOtp_returnsExpectedKey() {

        assertThat(RedisKeySpace.forPasswordResetOtp(ACCOUNT_ID))
            .isEqualTo("auth:password_reset_otp:" + ACCOUNT_ID);
    }

    @Test
    void forPasswordResetVerificationToken_returnsExpectedKey() {

        assertThat(RedisKeySpace.forPasswordResetVerificationToken(TOKEN))
            .isEqualTo("auth:password_reset_verification_token:" + TOKEN);
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

        var accountId = ACCOUNT_ID;

        assertThat(RedisKeySpace.forRateLimitUser(accountId))
            .isEqualTo("rate_limit:user:" + accountId);
    }

    @Test
    void forLoginAttempts_returnsExpectedKey() {

        assertThat(RedisKeySpace.forLoginAttempts(EMAIL))
            .isEqualTo("auth:login_attempts:" + EMAIL);
    }

    @Test
    void forLoginLockout_returnsExpectedKey() {

        assertThat(RedisKeySpace.forLoginLockout(EMAIL))
            .isEqualTo("auth:login_lockout:" + EMAIL);
    }
}
