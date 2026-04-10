-- Atomically blacklists an access token and deletes the associated refresh token.
-- The refresh token deletion is conditional: if it no longer exists, the script still succeeds.
-- Returns 1 always (the blacklist write is the mandatory operation).
--
-- KEYS[1] = auth:blacklist:{jti}
-- KEYS[2] = auth:refresh_token:{refreshToken}
-- ARGV[1] = "revoked" (blacklist value)
-- ARGV[2] = TTL in seconds for the blacklist entry
-- ARGV[3] = refreshToken (UUID, used for SREM)

if tonumber(ARGV[2]) > 0 then
    redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2])
end

local accountId = redis.call('GET', KEYS[2])
if accountId then
    redis.call('DEL', KEYS[2])
    redis.call('SREM', 'auth:user_tokens:' .. accountId, ARGV[3])
end

return 1
