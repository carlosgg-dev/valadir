-- Atomically stores a new refresh token and registers it in the user's token set.
-- Returns 1 always (the write is unconditional).
--
-- KEYS[1] = refresh_token:{token}
-- ARGV[1] = accountId (value stored for validation and used to build the user token set key)
-- ARGV[2] = TTL in seconds
-- ARGV[3] = token (UUID, used for SADD)

redis.call('SET', KEYS[1], ARGV[1], 'EX', tonumber(ARGV[2]))
redis.call('SADD', 'user:' .. ARGV[1] .. ':tokens', ARGV[3])

return 1
