-- Atomically stores a new refresh token and registers it in the user's token set. The token
-- itself never reaches Redis: it is addressed by its fingerprint, key and set member alike.
-- Returns 1 always (the write is unconditional).
--
-- KEYS[1] = auth:refresh_token:{fingerprint}
-- KEYS[2] = auth:user_tokens:{accountId}
-- ARGV[1] = accountId (value stored for validation)
-- ARGV[2] = TTL in seconds, for the token and for the set alike
-- ARGV[3] = fingerprint (used for SADD, and it must match the suffix of KEYS[1])

redis.call('SET', KEYS[1], ARGV[1], 'EX', tonumber(ARGV[2]))
redis.call('SADD', KEYS[2], ARGV[3])

-- Every refresh token lives the same span, so the one just added is the last of the set to die:
-- the set can share its TTL without ever outliving a live member. Nothing removes a fingerprint
-- when its token expires on its own, so without this the set would grow one dead member per login.
redis.call('EXPIRE', KEYS[2], tonumber(ARGV[2]))

return 1
