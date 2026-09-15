package com.valadir.security.adapter;

import com.valadir.application.port.out.PasswordResetVerification;
import com.valadir.application.port.out.PasswordResetVerificationTokenRepository;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.security.redis.RedisCircuitGuard;
import com.valadir.security.redis.RedisKeySpace;
import com.valadir.security.redis.TokenFingerprint;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PasswordResetVerificationTokenRepositoryRedisAdapter implements PasswordResetVerificationTokenRepository {

    private static final String ACCOUNT_ID_FIELD = "account_id";
    private static final String EMAIL_FIELD = "email";

    private final RedisOperations<String, String> redisOperations;
    private final RedisCircuitGuard circuitGuard;
    private final RedisScript<Long> saveVerificationScript;

    public PasswordResetVerificationTokenRepositoryRedisAdapter(
        RedisOperations<String, String> redisOperations,
        RedisCircuitGuard circuitGuard
    ) {

        this.redisOperations = redisOperations;
        this.circuitGuard = circuitGuard;
        this.saveVerificationScript = RedisScript.of(new ClassPathResource("scripts/save_password_reset_verification.lua"), Long.class);
    }

    @Override
    public void save(String verificationToken, PasswordResetVerification verification, Duration ttl) {

        circuitGuard.run("password reset OTP verification save failed", () ->
            redisOperations.execute(
                saveVerificationScript,
                List.of(redisKey(verificationToken)),
                verification.accountId().value().toString(),
                verification.email().value(),
                String.valueOf(ttl.toMillis())
            )
        );
    }

    @Override
    public Optional<PasswordResetVerification> verificationFor(String verificationToken) {

        return circuitGuard.call("password reset OTP verification lookup failed", () ->
            verificationOf(redisOperations.<String, String>opsForHash().multiGet(redisKey(verificationToken), List.of(ACCOUNT_ID_FIELD, EMAIL_FIELD)))
        );
    }

    @Override
    public void delete(String verificationToken) {

        circuitGuard.run("password reset OTP verification delete failed", () ->
            redisOperations.delete(redisKey(verificationToken))
        );
    }

    // Half a verification reads as none: a token must never reset a password without the address it was verified for
    private Optional<PasswordResetVerification> verificationOf(List<String> fields) {

        String accountId = fields.get(0);
        String email = fields.get(1);

        if (accountId == null || email == null) {
            return Optional.empty();
        }

        return Optional.of(new PasswordResetVerification(AccountId.from(UUID.fromString(accountId)), Email.from(email)));
    }

    private String redisKey(String verificationToken) {

        return RedisKeySpace.forPasswordResetVerificationToken(TokenFingerprint.of(verificationToken));
    }
}
