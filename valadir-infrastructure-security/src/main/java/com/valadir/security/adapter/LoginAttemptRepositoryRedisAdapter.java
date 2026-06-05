package com.valadir.security.adapter;

import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.domain.model.Email;
import com.valadir.domain.policy.LoginLockoutPolicy;
import com.valadir.security.redis.RedisKeySpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

// Fail-open on Redis outage: availability over brute-force enforcement
public class LoginAttemptRepositoryRedisAdapter implements LoginAttemptRepository {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptRepositoryRedisAdapter.class);

    private final RedisOperations<String, String> redisOperations;
    private final LoginLockoutPolicy policy;
    private final RedisScript<Long> recordLoginAttemptScript;

    public LoginAttemptRepositoryRedisAdapter(RedisOperations<String, String> redisOperations, LoginLockoutPolicy policy) {

        this.redisOperations = redisOperations;
        this.policy = policy;
        this.recordLoginAttemptScript = RedisScript.of(new ClassPathResource("scripts/login_attempt_increase_expire.lua"), Long.class);
    }

    @Override
    public Optional<Duration> findActiveLockout(Email email) {

        try {
            Long ttl = redisOperations.getExpire(lockoutKey(email), TimeUnit.SECONDS);
            if (ttl == null) {
                log.warn("Redis returned null TTL for lockout key — skipping lockout check for {}", email.value());
                return Optional.empty();
            }

            return ttl > 0 ? Optional.of(Duration.ofSeconds(ttl)) : Optional.empty();

        } catch (DataAccessException e) {
            log.warn("Redis unavailable — skipping lockout check for {}", email.value(), e);
            return Optional.empty();
        }
    }

    @Override
    public void recordFailedAttempt(Email email) {

        try {
            String attemptsKey = attemptsKey(email);
            Long count = redisOperations.execute(
                recordLoginAttemptScript,
                List.of(attemptsKey),
                String.valueOf(policy.attemptsWindow().getSeconds())
            );

            if (count == null) {
                log.warn("Redis script returned null for attempt count — failed attempt not recorded for {}", email.value());
                return;
            }

            Duration lockout = policy.lockoutFor(count);
            if (lockout.isPositive()) {
                redisOperations.opsForValue().set(lockoutKey(email), RedisKeySpace.LOGIN_LOCKOUT_VALUE, lockout);
            }

        } catch (DataAccessException e) {
            log.warn("Redis unavailable — failed attempt not recorded for {}", email.value(), e);
        }
    }

    @Override
    public void clearAttempts(Email email) {

        try {
            redisOperations.delete(List.of(attemptsKey(email), lockoutKey(email)));
        } catch (DataAccessException e) {
            log.warn("Redis unavailable — attempt counter not cleared for {}", email.value(), e);
        }
    }

    private String attemptsKey(Email email) {

        return RedisKeySpace.forLoginAttempts(email.value());
    }

    private String lockoutKey(Email email) {

        return RedisKeySpace.forLoginLockout(email.value());
    }
}
