package com.valadir.security.adapter;

import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.Email;
import com.valadir.domain.policy.LoginAttemptDecision;
import com.valadir.domain.policy.LoginLockoutPolicy;
import com.valadir.security.redis.RedisCircuitGuard;
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

public class LoginAttemptRepositoryRedisAdapter implements LoginAttemptRepository {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptRepositoryRedisAdapter.class);

    private final RedisOperations<String, String> redisOperations;
    private final RedisCircuitGuard circuitGuard;
    private final LoginLockoutPolicy policy;
    private final RedisScript<Long> recordLoginAttemptScript;

    public LoginAttemptRepositoryRedisAdapter(
        RedisOperations<String, String> redisOperations,
        RedisCircuitGuard circuitGuard,
        LoginLockoutPolicy policy
    ) {

        this.redisOperations = redisOperations;
        this.circuitGuard = circuitGuard;
        this.policy = policy;
        this.recordLoginAttemptScript = RedisScript.of(new ClassPathResource("scripts/login_attempt_increase_expire.lua"), Long.class);
    }

    @Override
    public LoginAttemptDecision evaluate(Email email) {

        return circuitGuard.call("login attempt evaluation failed", () -> {
            Long ttl = redisOperations.getExpire(lockoutKey(email), TimeUnit.SECONDS);
            if (ttl != null && ttl > 0) {
                return new LoginAttemptDecision.LockedOut(Duration.ofSeconds(ttl));
            }

            String countValue = redisOperations.opsForValue().get(attemptsKey(email));
            long count = countValue == null ? 0 : Long.parseLong(countValue);
            return policy.decideByCount(count);
        });
    }

    @Override
    public Optional<Duration> recordFailedAttempt(Email email) {

        return circuitGuard.call("failed login attempt was not recorded", () -> {
            Long count = redisOperations.execute(
                recordLoginAttemptScript,
                List.of(attemptsKey(email)),
                String.valueOf(policy.attemptsWindow().getSeconds())
            );

            if (count == null) {
                throw new InfrastructureException("Redis unavailable — attempt counter script returned no value");
            }

            Duration lockout = policy.lockoutFor(count);
            if (lockout.isPositive()) {
                redisOperations.opsForValue().set(lockoutKey(email), RedisKeySpace.LOGIN_LOCKOUT_VALUE, lockout);
                return Optional.of(lockout);
            }

            return Optional.empty();
        });
    }

    @Override
    public void clearAttempts(Email email) {

        try {
            redisOperations.delete(List.of(attemptsKey(email), lockoutKey(email)));
        } catch (DataAccessException e) {
            // The only failure of the three that is safe to swallow: a counter left uncleared makes the
            // next attempt more restrictive, never less. Denying a login that already proved its
            // credentials would add nothing.
            log.warn("Redis unavailable — attempt counter not cleared for {}", email.value(), e);
        }
    }

    private String lockoutKey(Email email) {

        return RedisKeySpace.forLoginLockout(email.value());
    }

    private String attemptsKey(Email email) {

        return RedisKeySpace.forLoginAttempts(email.value());
    }
}
