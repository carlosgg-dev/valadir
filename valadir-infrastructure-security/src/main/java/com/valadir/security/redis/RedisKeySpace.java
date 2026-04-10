package com.valadir.security.redis;

public final class RedisKeySpace {

    public static final String BLACKLIST_REVOKED_VALUE = "revoked";

    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_KEY_PREFIX = "user:";
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String RATE_LIMIT_IP_PREFIX = RATE_LIMIT_PREFIX + "ip:";
    private static final String RATE_LIMIT_EMAIL_PREFIX = RATE_LIMIT_PREFIX + "email:";
    private static final String RATE_LIMIT_USER_PREFIX = RATE_LIMIT_PREFIX + USER_TOKENS_KEY_PREFIX;

    private RedisKeySpace() {

    }

    public static String forBlacklist(final String jti) {

        return BLACKLIST_KEY_PREFIX + jti;
    }

    public static String forRefreshToken(final String token) {

        return REFRESH_TOKEN_KEY_PREFIX + token;
    }

    public static String forUserTokens(final String accountId) {

        return USER_TOKENS_KEY_PREFIX + accountId + ":tokens";
    }

    public static String forRateLimitIp(final String pathKey, final String ip) {

        return RATE_LIMIT_IP_PREFIX + pathKey + ":" + ip;
    }

    public static String forRateLimitEmail(final String pathKey, final String email) {

        return RATE_LIMIT_EMAIL_PREFIX + pathKey + ":" + email;
    }

    public static String forRateLimitUser(final String accountId) {

        return RATE_LIMIT_USER_PREFIX + accountId;
    }
}
