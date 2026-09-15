package com.valadir.security.adapter;

import com.valadir.application.port.out.EmailChangeRequest;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedOtp;
import com.valadir.security.redis.RedisKeySpace;
import com.valadir.test.containers.RedisContainerConfig;
import com.valadir.test.mother.OtpMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Objects;

import static com.valadir.security.redis.CircuitGuards.buildClosedCircuitGuard;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisContainerConfig.class)
class EmailChangeRequestRepositoryRedisAdapterIT {

    private static final Duration OTP_TTL = Duration.ofMinutes(15);
    private static final Email NEW_EMAIL = Email.from("matches.malone@email.com");

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private EmailChangeRequestRepositoryRedisAdapter adapter;

    @BeforeEach
    void setUp() {

        adapter = new EmailChangeRequestRepositoryRedisAdapter(redisTemplate, buildClosedCircuitGuard());
        RedisConnectionFactory factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
        try (var connection = factory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Test
    void save_request_expiresWithinTheTtl() {

        var accountId = AccountId.generate();

        adapter.save(accountId, new EmailChangeRequest(NEW_EMAIL, OtpMother.hashed()), OTP_TTL);

        assertThat(redisTemplate.getExpire(redisKeyOf(accountId))).isBetween(1L, OTP_TTL.toSeconds());
    }

    @Test
    void save_secondRequestForTheSameAccount_replacesTheFirst() {

        var accountId = AccountId.generate();
        var firstRequest = new EmailChangeRequest(NEW_EMAIL, OtpMother.hashed());
        var secondRequest = new EmailChangeRequest(Email.from("the.batman@email.com"), new HashedOtp("$argon2id$second"));

        adapter.save(accountId, firstRequest, OTP_TTL);
        adapter.save(accountId, secondRequest, OTP_TTL);

        assertThat(adapter.find(accountId)).contains(secondRequest);
    }

    @Test
    void save_twoAccounts_keepsEachRequestApart() {

        var accountId = AccountId.generate();
        var bystanderAccountId = AccountId.generate();
        var request = new EmailChangeRequest(NEW_EMAIL, OtpMother.hashed());
        var bystanderRequest = new EmailChangeRequest(Email.from("clark.kent@email.com"), new HashedOtp("$argon2id$bystander"));

        adapter.save(accountId, request, OTP_TTL);
        adapter.save(bystanderAccountId, bystanderRequest, OTP_TTL);
        adapter.delete(accountId);

        assertThat(adapter.find(bystanderAccountId)).contains(bystanderRequest);
    }

    @Test
    void find_savedRequest_returnsTheAddressAndTheCodeTogether() {

        var accountId = AccountId.generate();
        var request = new EmailChangeRequest(NEW_EMAIL, OtpMother.hashed());

        adapter.save(accountId, request, OTP_TTL);

        assertThat(adapter.find(accountId)).contains(request);
    }

    @Test
    void find_noRequest_returnsEmpty() {

        assertThat(adapter.find(AccountId.generate())).isEmpty();
    }

    @Test
    void find_requestMissingTheEmail_returnsEmpty() {

        var accountId = AccountId.generate();

        redisTemplate.<String, String>opsForHash().put(redisKeyOf(accountId), "hashed_otp", OtpMother.hashed().value());

        assertThat(adapter.find(accountId)).isEmpty();
    }

    @Test
    void find_requestMissingTheOtp_returnsEmpty() {

        var accountId = AccountId.generate();

        redisTemplate.<String, String>opsForHash().put(redisKeyOf(accountId), "new_email", NEW_EMAIL.value());

        assertThat(adapter.find(accountId)).isEmpty();
    }

    @Test
    void delete_existingRequest_removesIt() {

        var accountId = AccountId.generate();

        adapter.save(accountId, new EmailChangeRequest(NEW_EMAIL, OtpMother.hashed()), OTP_TTL);
        adapter.delete(accountId);

        assertThat(redisTemplate.hasKey(redisKeyOf(accountId))).isFalse();
    }

    private static String redisKeyOf(AccountId accountId) {

        return RedisKeySpace.forEmailChangeRequest(accountId.value().toString());
    }
}
