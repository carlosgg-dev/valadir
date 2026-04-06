package com.valadir.security.adapter;

import com.valadir.application.result.TokenValidationResult;
import com.valadir.domain.model.AccountId;
import com.valadir.security.RedisTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenRedisAdapterTest extends RedisTestContainer {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_KEY_PREFIX = "user:";
    private static final String USER_TOKENS_KEY_SUFFIX = ":tokens";

    @Autowired
    private RefreshTokenRedisAdapter adapter;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {

        final RedisConnectionFactory factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
        try (final var connection = factory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Test
    void validate_existingToken_returnsValid() {

        final var accountId = AccountId.generate();
        final var token = UUID.randomUUID().toString();

        adapter.save(token, accountId);
        final TokenValidationResult result = adapter.validate(token);

        assertThat(result).isInstanceOf(TokenValidationResult.Valid.class);
        assertThat(((TokenValidationResult.Valid) result).accountId()).isEqualTo(accountId);
    }

    @Test
    void validate_nonExistingToken_returnsInvalid() {

        final TokenValidationResult result = adapter.validate(UUID.randomUUID().toString());

        assertThat(result).isInstanceOf(TokenValidationResult.Invalid.class);
    }

    @Test
    void save_token_isStoredWithAccountIdAndAddedToUserSet() {

        final var accountId = AccountId.generate();
        final var token = UUID.randomUUID().toString();

        adapter.save(token, accountId);

        assertThat(redisTemplate.opsForValue().get(REFRESH_TOKEN_KEY_PREFIX + token)).isEqualTo(accountId.value().toString());
        assertThat(redisTemplate.opsForSet().isMember(USER_TOKENS_KEY_PREFIX + accountId.value() + USER_TOKENS_KEY_SUFFIX, token)).isTrue();
    }

    @Test
    void delete_existingToken_removesTokenAndUserSetEntry() {

        final var accountId = AccountId.generate();
        final var token = UUID.randomUUID().toString();

        adapter.save(token, accountId);
        assertThat(redisTemplate.opsForSet().isMember(USER_TOKENS_KEY_PREFIX + accountId.value() + USER_TOKENS_KEY_SUFFIX, token)).isTrue();

        adapter.delete(token);

        assertThat(redisTemplate.opsForValue().get(REFRESH_TOKEN_KEY_PREFIX + token)).isNull();
        assertThat(redisTemplate.opsForSet().isMember(USER_TOKENS_KEY_PREFIX + accountId.value() + USER_TOKENS_KEY_SUFFIX, token)).isFalse();
    }

    @Test
    void validate_afterDelete_returnsInvalid() {

        final var accountId = AccountId.generate();
        final var token = UUID.randomUUID().toString();

        adapter.save(token, accountId);
        adapter.delete(token);

        assertThat(adapter.validate(token)).isInstanceOf(TokenValidationResult.Invalid.class);
    }
}
