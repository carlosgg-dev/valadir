package com.valadir.security.redis;

public final class RedisKeySpace {

    public static final String BLACKLIST_REVOKED_VALUE = "revoked";

    private static final String AUTH_SPACE_PREFIX = "auth:";
    private static final String AUTH_BLACKLIST_PREFIX = AUTH_SPACE_PREFIX + "blacklist:";
    private static final String AUTH_REFRESH_TOKEN_PREFIX = AUTH_SPACE_PREFIX + "refresh_token:";
    private static final String AUTH_USER_TOKENS_PREFIX = AUTH_SPACE_PREFIX + "user_tokens:";

    private static final String RATE_LIMIT_SPACE_PREFIX = "rate_limit:";
    private static final String RATE_LIMIT_IP_PREFIX = RATE_LIMIT_SPACE_PREFIX + "ip:";
    private static final String RATE_LIMIT_EMAIL_PREFIX = RATE_LIMIT_SPACE_PREFIX + "email:";
    private static final String RATE_LIMIT_USER_PREFIX = RATE_LIMIT_SPACE_PREFIX + "user:";

    private RedisKeySpace() {

    }

    public static String forBlacklist(final String jti) {

        return AUTH_BLACKLIST_PREFIX + jti;
    }

    public static String forRefreshToken(final String token) {

        return AUTH_REFRESH_TOKEN_PREFIX + token;
    }

    public static String forUserTokens(final String accountId) {

        return AUTH_USER_TOKENS_PREFIX + accountId;
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
