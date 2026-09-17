package com.valadir.security.redis;

import com.valadir.common.ratelimit.RateLimitSubject;

import java.util.Locale;
import java.util.regex.Pattern;

public final class RedisKeySpace {

    private static final String AUTH_SPACE_PREFIX = "auth:";

    public static final String BLACKLIST_REVOKED_VALUE = "revoked";
    public static final String LOGIN_LOCKOUT_VALUE = "locked";
    public static final String REFRESH_TOKEN_PREFIX = AUTH_SPACE_PREFIX + "refresh_token:";

    private static final String RATE_LIMIT_SPACE_PREFIX = "rate_limit:";

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern SURROUNDING_UNDERSCORES = Pattern.compile("^_+|_+$");

    private RedisKeySpace() {

    }

    public static String forBlacklist(String jti) {

        return AUTH_SPACE_PREFIX + "blacklist:" + jti;
    }

    public static String forRefreshToken(TokenFingerprint fingerprint) {

        return REFRESH_TOKEN_PREFIX + fingerprint.value();
    }

    public static String forUserTokens(String accountId) {

        return AUTH_SPACE_PREFIX + "user_tokens:" + accountId;
    }

    public static String forTokenCutoff(String accountId) {

        return AUTH_SPACE_PREFIX + "token_cutoff:" + accountId;
    }

    public static String forAccountActivationOtp(String accountId) {

        return AUTH_SPACE_PREFIX + "account_activation_otp:" + accountId;
    }

    public static String forPasswordResetOtp(String accountId) {

        return AUTH_SPACE_PREFIX + "password_reset_otp:" + accountId;
    }

    public static String forPasswordResetVerificationToken(TokenFingerprint fingerprint) {

        return AUTH_SPACE_PREFIX + "password_reset_verification_token:" + fingerprint.value();
    }

    public static String forEmailChangeRequest(String accountId) {

        return AUTH_SPACE_PREFIX + "email_change:" + accountId;
    }

    public static String forRateLimit(RateLimitSubject subject) {

        return RATE_LIMIT_SPACE_PREFIX
            + subject.strategy().name().toLowerCase(Locale.ROOT) + ":"
            + normalizeScope(subject.scope()) + ":"
            + subject.value();
    }

    public static String forLoginAttempts(String email) {

        return AUTH_SPACE_PREFIX + "login_attempts:" + email;
    }

    public static String forLoginLockout(String email) {

        return AUTH_SPACE_PREFIX + "login_lockout:" + email;
    }

    // The scope arrives as a route, which carries separators a key should not: one spelling per
    // route, whatever the rule was written with.
    private static String normalizeScope(String scope) {

        String normalized = NON_ALPHANUMERIC.matcher(scope.toLowerCase(Locale.ROOT)).replaceAll("_");
        return SURROUNDING_UNDERSCORES.matcher(normalized).replaceAll("");
    }
}
