-- Atomically rotates a refresh token: deletes the old one and saves the new one.
-- Returns 1 if the rotation succeeded, 0 if the old token did not exist.
--
-- KEYS[1] = refresh_token:{oldToken}
-- KEYS[2] = refresh_token:{newToken}
-- ARGV[1] = oldToken (UUID, used for SREM)
-- ARGV[2] = newToken (UUID, used for SADD)
-- ARGV[3] = accountId (value stored for the new token)
-- ARGV[4] = TTL in seconds for the new token

local accountId = redis.call('GET', KEYS[1])
if not accountId then
    return 0
end

redis.call('DEL', KEYS[1])
redis.call('SREM', 'user:' .. accountId .. ':tokens', ARGV[1])
redis.call('SET', KEYS[2], ARGV[3], 'EX', ARGV[4])
redis.call('SADD', 'user:' .. accountId .. ':tokens', ARGV[2])

return 1
