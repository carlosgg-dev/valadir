-- Atomically stores a password reset verification. The expiry lands with the fields, so no failure in between can
-- leave a token that never expires.
-- Returns 1 always (the write is unconditional).
--
-- KEYS[1] = auth:password_reset_verification_token:{fingerprint}
-- ARGV[1] = account id
-- ARGV[2] = email the code was verified for
-- ARGV[3] = TTL in milliseconds

redis.call('HSET', KEYS[1], 'account_id', ARGV[1], 'email', ARGV[2])
redis.call('PEXPIRE', KEYS[1], ARGV[3])

return 1
