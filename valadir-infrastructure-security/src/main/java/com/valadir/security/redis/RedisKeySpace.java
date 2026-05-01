package com.valadir.security.redis;

public final class RedisKeySpace {

    public static final String BLACKLIST_REVOKED_VALUE = "revoked";
    public static final String LOGIN_LOCKOUT_VALUE = "locked";

    private static final String AUTH_SPACE_PREFIX = "auth:";
    private static final String RATE_LIMIT_SPACE_PREFIX = "rate_limit:";

    private RedisKeySpace() {

    }

    public static String forBlacklist(String jti) {

        return AUTH_SPACE_PREFIX + "blacklist:" + jti;
    }

    public static String forRefreshToken(String token) {

        return AUTH_SPACE_PREFIX + "refresh_token:" + token;
    }

    public static String forUserTokens(String accountId) {

        return AUTH_SPACE_PREFIX + "user_tokens:" + accountId;
    }

    public static String forVerificationOtp(String accountId) {

        return AUTH_SPACE_PREFIX + "verification_otp:" + accountId;
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
