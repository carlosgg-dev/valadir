package com.valadir.security.redis;

import com.valadir.common.ratelimit.RateLimitStrategy;
import com.valadir.common.ratelimit.RateLimitSubject;
import com.valadir.domain.model.AccountId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class RedisKeySpaceTest {

    private static final String JTI = "abc-123";
    private static final TokenFingerprint FINGERPRINT = TokenFingerprint.of("refresh-token-xyz");
    private static final String ROUTE = "/api/auth/login/";
    private static final String PATH_KEY = "api_auth_login";
    private static final String IP = "192.168.1.1";
    private static final String EMAIL = "user@example.com";
    private static final String ACCOUNT_ID = AccountId.generate().value().toString();

    // Case folding is locale dependent: in this one an uppercase I folds to a dotless "ı" instead
    // of "i", which the normalizer then rejects as non alphanumeric.
    private static final Locale LOCALE_WITH_DIFFERENT_CASE_FOLDING = Locale.forLanguageTag("tr");

    @Test
    void forBlacklist_returnsExpectedKey() {

        assertThat(RedisKeySpace.forBlacklist(JTI)).isEqualTo("auth:blacklist:" + JTI);
    }

    @Test
    void forRefreshToken_returnsExpectedKey() {

        assertThat(RedisKeySpace.forRefreshToken(FINGERPRINT))
            .isEqualTo("auth:refresh_token:" + FINGERPRINT.value());
    }

    @Test
    void forUserTokens_returnsExpectedKey() {

        assertThat(RedisKeySpace.forUserTokens(ACCOUNT_ID))
            .isEqualTo("auth:user_tokens:" + ACCOUNT_ID);
    }

    @Test
    void forTokenCutoff_returnsExpectedKey() {

        assertThat(RedisKeySpace.forTokenCutoff(ACCOUNT_ID))
            .isEqualTo("auth:token_cutoff:" + ACCOUNT_ID);
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

        assertThat(RedisKeySpace.forPasswordResetVerificationToken(FINGERPRINT))
            .isEqualTo("auth:password_reset_verification_token:" + FINGERPRINT.value());
    }

    @Test
    void forEmailChangeRequest_returnsExpectedKey() {

        assertThat(RedisKeySpace.forEmailChangeRequest(ACCOUNT_ID))
            .isEqualTo("auth:email_change:" + ACCOUNT_ID);
    }

    @Test
    void forRateLimit_ipSubject_returnsExpectedKey() {

        assertThat(RedisKeySpace.forRateLimit(new RateLimitSubject(RateLimitStrategy.IP, ROUTE, IP)))
            .isEqualTo("rate_limit:ip:" + PATH_KEY + ":" + IP);
    }

    @Test
    void forRateLimit_emailSubject_returnsExpectedKey() {

        assertThat(RedisKeySpace.forRateLimit(new RateLimitSubject(RateLimitStrategy.EMAIL, ROUTE, EMAIL)))
            .isEqualTo("rate_limit:email:" + PATH_KEY + ":" + EMAIL);
    }

    @Test
    void forRateLimit_userSubject_returnsExpectedKey() {

        assertThat(RedisKeySpace.forRateLimit(new RateLimitSubject(RateLimitStrategy.USER, ROUTE, ACCOUNT_ID)))
            .isEqualTo("rate_limit:user:" + PATH_KEY + ":" + ACCOUNT_ID);
    }

    // However the rule spells the route, the bucket it names is the same one.
    @ParameterizedTest(name = "{0} \u2192 {1}")
    @CsvSource({
        "/api/auth/login, api_auth_login",
        "api/auth/login/, api_auth_login",
        "/api/auth/login/, api_auth_login",
        "api/auth/login, api_auth_login",
        "/API/AUTH/LOGIN, api_auth_login",
        "/api/v2/users/profile, api_v2_users_profile",
        "/api//double-slash, api_double_slash"
    })
    void forRateLimit_normalizesTheRouteIntoASingleScope(String route, String expectedScope) {

        assertThat(RedisKeySpace.forRateLimit(new RateLimitSubject(RateLimitStrategy.IP, route, IP)))
            .isEqualTo("rate_limit:ip:" + expectedScope + ":" + IP);
    }

    // "/API/AUTH/LOGIN" would normalize to "ap_auth_log_n" instead of "api_auth_login", rate
    // limiting the same endpoint under a different Redis key depending on where the JVM runs.
    @Test
    void forRateLimit_normalizesTheRouteIndependentlyOfTheDefaultLocale() {

        Locale defaultLocale = Locale.getDefault();

        try {
            Locale.setDefault(LOCALE_WITH_DIFFERENT_CASE_FOLDING);

            assertThat(RedisKeySpace.forRateLimit(new RateLimitSubject(RateLimitStrategy.IP, "/API/AUTH/LOGIN", IP)))
                .isEqualTo("rate_limit:ip:" + PATH_KEY + ":" + IP);

        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    // The strategy names the key segment, and an uppercase I must not fold to a dotless one.
    @Test
    void forRateLimit_spellsTheStrategyIndependentlyOfTheDefaultLocale() {

        Locale defaultLocale = Locale.getDefault();

        try {
            Locale.setDefault(LOCALE_WITH_DIFFERENT_CASE_FOLDING);

            assertThat(RedisKeySpace.forRateLimit(new RateLimitSubject(RateLimitStrategy.IP, ROUTE, IP)))
                .startsWith("rate_limit:ip:");

        } finally {
            Locale.setDefault(defaultLocale);
        }
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
