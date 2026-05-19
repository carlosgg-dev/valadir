-- Atomically revokes all refresh tokens for an account.
-- Reads the user token set, deletes each token, then deletes the set.
-- Returns the number of tokens revoked.
--
-- KEYS[1] = auth:user_tokens:{accountId}
-- ARGV[1] = auth:refresh_token: (prefix used to build each token key)

local tokens = redis.call('SMEMBERS', KEYS[1])
for _, token in ipairs(tokens) do
    redis.call('DEL', ARGV[1] .. token)
end
redis.call('DEL', KEYS[1])

return #tokens
