-- Atomically blacklists an access token and removes the refresh token from the user token set.
-- The refresh token removal is conditional: if it no longer exists, the script still succeeds.
-- Returns 1 always (the blacklist write is the mandatory operation).
--
-- KEYS[1] = auth:blacklist:{jti}
-- KEYS[2] = auth:refresh_token:{refreshToken}
-- KEYS[3] = auth:user_tokens:{accountId}
-- ARGV[1] = "revoked" (blacklist value)
-- ARGV[2] = TTL in seconds for the blacklist entry
-- ARGV[3] = refreshToken (UUID, used for SREM)

if tonumber(ARGV[2]) > 0 then
    redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2])
end

local exists = redis.call('EXISTS', KEYS[2])
if exists == 1 then
    redis.call('DEL', KEYS[2])
    redis.call('SREM', KEYS[3], ARGV[3])
end

return 1
