package com.valadir.security.adapter;

import com.valadir.application.result.TokenValidationResult;
import com.valadir.domain.model.AccountId;
import com.valadir.security.RedisTestContainer;
import com.valadir.security.redis.RedisKeySpace;
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

    @Autowired
    private RefreshTokenRedisAdapter adapter;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {

        RedisConnectionFactory factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
        try (var connection = factory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Test
    void validate_existingToken_returnsValid() {

        var accountId = AccountId.generate();
        var token = UUID.randomUUID().toString();

        adapter.save(token, accountId);
        TokenValidationResult result = adapter.validate(token);

        assertThat(result).isInstanceOf(TokenValidationResult.Valid.class);
        assertThat(((TokenValidationResult.Valid) result).accountId()).isEqualTo(accountId);
    }

    @Test
    void validate_nonExistingToken_returnsInvalid() {

        TokenValidationResult result = adapter.validate(UUID.randomUUID().toString());

        assertThat(result).isInstanceOf(TokenValidationResult.Invalid.class);
    }

    @Test
    void save_token_isStoredWithAccountIdAndAddedToUserSet() {

        var accountId = AccountId.generate();
        var accountIdStr = accountId.value().toString();
        var token = UUID.randomUUID().toString();

        adapter.save(token, accountId);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(token))).isEqualTo(accountIdStr);
        assertThat(redisTemplate.opsForSet().isMember(RedisKeySpace.forUserTokens(accountIdStr), token)).isTrue();
    }

    @Test
    void rotate_existingToken_replacesOldWithNew() {

        var accountId = AccountId.generate();
        var accountIdStr = accountId.value().toString();
        var oldToken = UUID.randomUUID().toString();
        var newToken = UUID.randomUUID().toString();

        adapter.save(oldToken, accountId);

        boolean rotated = adapter.rotate(oldToken, newToken, accountId);

        assertThat(rotated).isTrue();
        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(oldToken))).isNull();
        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(newToken))).isEqualTo(accountIdStr);
        assertThat(redisTemplate.opsForSet().isMember(RedisKeySpace.forUserTokens(accountIdStr), oldToken)).isFalse();
        assertThat(redisTemplate.opsForSet().isMember(RedisKeySpace.forUserTokens(accountIdStr), newToken)).isTrue();
    }

    @Test
    void rotate_nonExistingToken_returnsFalseAndLeavesNoState() {

        var accountId = AccountId.generate();
        var nonExistingToken = UUID.randomUUID().toString();
        var newToken = UUID.randomUUID().toString();

        boolean rotated = adapter.rotate(nonExistingToken, newToken, accountId);

        assertThat(rotated).isFalse();
        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(newToken))).isNull();
    }

}
