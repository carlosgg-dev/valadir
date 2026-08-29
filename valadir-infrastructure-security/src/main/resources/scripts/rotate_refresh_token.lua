-- Atomically rotates a refresh token: deletes the old one and saves the new one.
-- Returns 1 if the rotation succeeded, 0 if the old token did not exist.
--
-- KEYS[1] = auth:refresh_token:{oldFingerprint}
-- KEYS[2] = auth:refresh_token:{newFingerprint}
-- KEYS[3] = auth:user_tokens:{accountId}
-- ARGV[1] = oldFingerprint (used for SREM)
-- ARGV[2] = newFingerprint (used for SADD)
-- ARGV[3] = accountId (value stored for the new token)
-- ARGV[4] = TTL in seconds, for the new token and for the set alike

local accountId = redis.call('GET', KEYS[1])
if not accountId then
    return 0
end

redis.call('DEL', KEYS[1])
redis.call('SREM', KEYS[3], ARGV[1])
redis.call('SET', KEYS[2], ARGV[3], 'EX', ARGV[4])
redis.call('SADD', KEYS[3], ARGV[2])

-- The rotation renews the set's lifetime the same way a save does: the member just added is the
-- last of the set to die. A session that only ever rotates would otherwise leave it unbounded.
redis.call('EXPIRE', KEYS[3], tonumber(ARGV[4]))

return 1
