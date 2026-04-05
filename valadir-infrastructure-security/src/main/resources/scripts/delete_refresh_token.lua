-- Atomically deletes a refresh token and removes it from the user's token set.
-- Returns 1 if the token existed and was deleted, 0 if it did not exist.
--
-- KEYS[1] = refresh_token:{token}
-- ARGV[1] = token (UUID, used for SREM)

local accountId = redis.call('GET', KEYS[1])
if not accountId then
    return 0
end

redis.call('DEL', KEYS[1])
redis.call('SREM', 'user:' .. accountId .. ':tokens', ARGV[1])

return 1
