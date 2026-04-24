package com.valadir.security.adapter;

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

import java.time.Duration;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OtpRedisAdapterTest extends RedisTestContainer {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private OtpRedisAdapter adapter;

    @BeforeEach
    void setUp() {

        adapter = new OtpRedisAdapter(redisTemplate);
        RedisConnectionFactory factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
        try (var connection = factory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Test
    void save_storesHashedOtpInRedis() {

        var accountId = AccountId.generate();
        var hashedOtp = "$argon2id$hashedOtp";
        String redisKey = RedisKeySpace.forVerificationOtp(accountId.value().toString());

        adapter.save(accountId, hashedOtp, Duration.ofMinutes(10));

        assertThat(redisTemplate.opsForValue().get(redisKey)).isEqualTo(hashedOtp);
    }

    @Test
    void save_withTtl_ttlIsApplied() {

        var accountId = AccountId.generate();
        var hashedOtp = "$argon2id$hashedOtp";
        String redisKey = RedisKeySpace.forVerificationOtp(accountId.value().toString());

        adapter.save(accountId, hashedOtp, Duration.ofMinutes(10));

        Long remaining = redisTemplate.getExpire(redisKey);
        assertThat(remaining).isGreaterThan(0);
    }

    @Test
    void find_existingOtp_returnsIt() {

        var accountId = AccountId.generate();
        var hashedOtp = "$argon2id$hashedOtp";

        adapter.save(accountId, hashedOtp, Duration.ofMinutes(10));

        assertThat(adapter.find(accountId)).contains(hashedOtp);
    }

    @Test
    void find_nonExistingOtp_returnsEmpty() {

        assertThat(adapter.find(AccountId.generate())).isEmpty();
    }

    @Test
    void delete_existingOtp_removesItFromRedis() {

        var accountId = AccountId.generate();
        var hashedOtp = "$argon2id$hashedOtp";
        adapter.save(accountId, hashedOtp, Duration.ofMinutes(10));

        assertThat(adapter.find(accountId)).contains(hashedOtp);

        adapter.delete(accountId);

        assertThat(adapter.find(accountId)).isEmpty();
    }
}
