package com.valadir.security.redis;

import com.valadir.domain.model.AccountId;

public final class RedisKeySpace {

    public static final String BLACKLIST_REVOKED_VALUE = "revoked";

    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_KEY_PREFIX = "user:";
    private static final String USER_TOKENS_KEY_SUFFIX = ":tokens";

    private RedisKeySpace() {

    }

    public static String forBlacklist(final String jti) {

        return BLACKLIST_KEY_PREFIX + jti;
    }

    public static String forRefreshToken(final String token) {

        return REFRESH_TOKEN_KEY_PREFIX + token;
    }

    public static String forUserTokens(final AccountId accountId) {

        return USER_TOKENS_KEY_PREFIX + accountId.value() + USER_TOKENS_KEY_SUFFIX;
    }
}
