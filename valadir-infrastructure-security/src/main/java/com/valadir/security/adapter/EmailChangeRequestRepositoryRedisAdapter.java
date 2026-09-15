package com.valadir.security.adapter;

import com.valadir.application.port.out.EmailChangeRequest;
import com.valadir.application.port.out.EmailChangeRequestRepository;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedOtp;
import com.valadir.security.redis.RedisCircuitGuard;
import com.valadir.security.redis.RedisKeySpace;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class EmailChangeRequestRepositoryRedisAdapter implements EmailChangeRequestRepository {

    private static final String NEW_EMAIL_FIELD = "new_email";
    private static final String HASHED_OTP_FIELD = "hashed_otp";

    private final RedisOperations<String, String> redisOperations;
    private final RedisCircuitGuard circuitGuard;
    private final RedisScript<Long> saveEmailChangeRequestScript;

    public EmailChangeRequestRepositoryRedisAdapter(RedisOperations<String, String> redisOperations, RedisCircuitGuard circuitGuard) {

        this.redisOperations = redisOperations;
        this.circuitGuard = circuitGuard;
        this.saveEmailChangeRequestScript = RedisScript.of(new ClassPathResource("scripts/save_email_change_request.lua"), Long.class);
    }

    @Override
    public void save(AccountId accountId, EmailChangeRequest request, Duration ttl) {

        circuitGuard.run("email change request save failed", () ->
            redisOperations.execute(
                saveEmailChangeRequestScript,
                List.of(redisKey(accountId)),
                request.newEmail().value(),
                request.hashedOtp().value(),
                String.valueOf(ttl.toMillis())
            )
        );
    }

    @Override
    public Optional<EmailChangeRequest> find(AccountId accountId) {

        return circuitGuard.call("email change request lookup failed", () ->
            requestOf(redisOperations.<String, String>opsForHash().multiGet(redisKey(accountId), List.of(NEW_EMAIL_FIELD, HASHED_OTP_FIELD)))
        );
    }

    @Override
    public void delete(AccountId accountId) {

        circuitGuard.run("email change request delete failed", () -> redisOperations.delete(redisKey(accountId)));
    }

    // Half a request reads as none: a code must never confirm a change without the address it was sent to
    private Optional<EmailChangeRequest> requestOf(List<String> fields) {

        String newEmail = fields.get(0);
        String hashedOtp = fields.get(1);

        if (newEmail == null || hashedOtp == null) {
            return Optional.empty();
        }

        return Optional.of(new EmailChangeRequest(Email.from(newEmail), new HashedOtp(hashedOtp)));
    }

    private String redisKey(AccountId accountId) {

        return RedisKeySpace.forEmailChangeRequest(accountId.value().toString());
    }
}
