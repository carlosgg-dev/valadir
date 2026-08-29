package com.valadir.test.redis;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.RedisOperations;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

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

    public static List<String> everythingStoredIn(RedisOperations<String, String> redisOperations) {

        return requireNonNull(redisOperations.keys("*")).stream()
            .flatMap(key -> Stream.concat(Stream.of(key), contentsAt(redisOperations, key)))
            .toList();
    }

    private static Stream<String> contentsAt(RedisOperations<String, String> redisOperations, String key) {

        DataType type = requireNonNull(redisOperations.type(key));

        return switch (type) {
            case NONE -> Stream.empty();
            case STRING -> Optional.ofNullable(redisOperations.opsForValue().get(key)).stream();
            case SET -> requireNonNull(redisOperations.opsForSet().members(key)).stream();
            // A type this codebase does not use would read as nothing, and a secret stored in it
            // would pass the sweep unnoticed.
            default -> throw new IllegalStateException("Unsupported Redis type %s at key %s".formatted(type, key));
        };
    }
}
