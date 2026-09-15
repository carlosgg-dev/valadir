-- Atomically replaces the pending email change of an account. The expiry lands with the fields, so no failure
-- in between can leave a request whose code never expires.
-- Returns 1 always (the write is unconditional).
--
-- KEYS[1] = auth:email_change:{accountId}
-- ARGV[1] = new email
-- ARGV[2] = hashed OTP
-- ARGV[3] = TTL in milliseconds

redis.call('DEL', KEYS[1])
redis.call('HSET', KEYS[1], 'new_email', ARGV[1], 'hashed_otp', ARGV[2])
redis.call('PEXPIRE', KEYS[1], ARGV[3])

return 1
