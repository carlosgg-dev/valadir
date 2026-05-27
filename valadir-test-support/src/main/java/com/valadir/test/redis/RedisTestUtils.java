package com.valadir.test.redis;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;

import java.lang.reflect.Proxy;

public final class RedisTestUtils {

    private static final DataAccessException REDIS_ERROR = new DataAccessException("Redis unavailable") {
    };

    private RedisTestUtils() {

    }

    @SuppressWarnings("unchecked")
    public static RedisOperations<String, String> errorTemplate() {

        return (RedisOperations<String, String>) Proxy.newProxyInstance(
            RedisOperations.class.getClassLoader(),
            new Class[]{RedisOperations.class},
            (proxy, method, args) -> {
                throw REDIS_ERROR;
            }
        );
    }
}
