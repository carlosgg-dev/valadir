package com.valadir.security.redis;

public final class RedisKeySpace {

    private static final String AUTH_SPACE_PREFIX = "auth:";

    public static final String BLACKLIST_REVOKED_VALUE = "revoked";
    public static final String LOGIN_LOCKOUT_VALUE = "locked";
    public static final String REFRESH_TOKEN_PREFIX = AUTH_SPACE_PREFIX + "refresh_token:";

    private static final String RATE_LIMIT_SPACE_PREFIX = "rate_limit:";

    private RedisKeySpace() {

    }

    public static String forBlacklist(String jti) {

        return AUTH_SPACE_PREFIX + "blacklist:" + jti;
    }

    public static String forRefreshToken(String token) {

        return REFRESH_TOKEN_PREFIX + token;
    }

    public static String forUserTokens(String accountId) {

        return AUTH_SPACE_PREFIX + "user_tokens:" + accountId;
    }

    public static String forAccountActivationOtp(String accountId) {

        return AUTH_SPACE_PREFIX + "account_activation_otp:" + accountId;
    }

    public static String forPasswordResetOtp(String accountId) {

        return AUTH_SPACE_PREFIX + "password_reset_otp:" + accountId;
    }

    public static String forPasswordResetVerificationToken(String token) {

        return AUTH_SPACE_PREFIX + "password_reset_verification_token:" + token;
    }

    public static String forRateLimitIp(String pathKey, String ip) {

        return RATE_LIMIT_SPACE_PREFIX + "ip:" + pathKey + ":" + ip;
    }

    public static String forRateLimitEmail(String pathKey, String email) {

        return RATE_LIMIT_SPACE_PREFIX + "email:" + pathKey + ":" + email;
    }

    public static String forRateLimitUser(String accountId) {

        return RATE_LIMIT_SPACE_PREFIX + "user:" + accountId;
    }

    public static String forLoginAttempts(String email) {

        return AUTH_SPACE_PREFIX + "login_attempts:" + email;
    }

    public static String forLoginLockout(String email) {

        return AUTH_SPACE_PREFIX + "login_lockout:" + email;
    }
}
