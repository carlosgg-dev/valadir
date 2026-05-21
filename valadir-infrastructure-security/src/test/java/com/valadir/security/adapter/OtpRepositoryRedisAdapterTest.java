package com.valadir.security.adapter;

import com.valadir.application.otp.HashedOtp;
import com.valadir.domain.model.AccountId;
import com.valadir.security.RedisTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Objects;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OtpRepositoryRedisAdapterTest extends RedisTestContainer {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final UnaryOperator<String> REDIS_KEY_FN = id -> "test:otp:" + id;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private OtpRepositoryRedisAdapter adapter;

    @BeforeEach
    void setUp() {

        adapter = new OtpRepositoryRedisAdapter(redisTemplate, REDIS_KEY_FN);
        RedisConnectionFactory factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
        try (var connection = factory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Test
    void save_savesHashedOtpInRedis() {

        var accountId = AccountId.generate();
        var hashedOtp = new HashedOtp("$argon2id$hashedOtp");
        String redisKey = REDIS_KEY_FN.apply(accountId.value().toString());

        adapter.save(accountId, hashedOtp, OTP_TTL);

        assertThat(redisTemplate.opsForValue().get(redisKey)).isEqualTo(hashedOtp.value());
    }

    @Test
    void save_withTtl_ttlIsApplied() {

        var accountId = AccountId.generate();
        var hashedOtp = new HashedOtp("$argon2id$hashedOtp");
        String redisKey = REDIS_KEY_FN.apply(accountId.value().toString());

        adapter.save(accountId, hashedOtp, OTP_TTL);

        Long remaining = redisTemplate.getExpire(redisKey);
        assertThat(remaining).isGreaterThan(0);
    }

    @Test
    void find_existingOtp_returnsIt() {

        var accountId = AccountId.generate();
        var hashedOtp = new HashedOtp("$argon2id$hashedOtp");

        adapter.save(accountId, hashedOtp, OTP_TTL);

        assertThat(adapter.find(accountId)).contains(hashedOtp);
    }

    @Test
    void find_nonExistingOtp_returnsEmpty() {

        assertThat(adapter.find(AccountId.generate())).isEmpty();
    }

    @Test
    void delete_existingOtp_removesItFromRedis() {

        var accountId = AccountId.generate();
        var hashedOtp = new HashedOtp("$argon2id$hashedOtp");

        adapter.save(accountId, hashedOtp, OTP_TTL);

        assertThat(adapter.find(accountId)).contains(hashedOtp);

        adapter.delete(accountId);

        assertThat(adapter.find(accountId)).isEmpty();
    }
}
