-- Atomically blacklists an access token and removes the refresh token from the user token set.
-- The refresh token removal is conditional: it only happens when the token belongs to the account
-- logging out, so a token from another account cannot be revoked through this endpoint. If it no
-- longer exists, or is not owned by that account, the script still succeeds.
-- Returns 1 always (the blacklist write is the mandatory operation).
--
-- KEYS[1] = auth:blacklist:{jti}
-- KEYS[2] = auth:refresh_token:{fingerprint}
-- KEYS[3] = auth:user_tokens:{accountId}
-- ARGV[1] = "revoked" (blacklist value)
-- ARGV[2] = TTL in seconds for the blacklist entry
-- ARGV[3] = fingerprint of the refresh token (used for SREM)
-- ARGV[4] = accountId (owner of the session, matched against the stored token value)

if tonumber(ARGV[2]) > 0 then
    redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2])
end

-- GET on a missing key yields false, which never equals a string: an unknown token skips the branch.
if redis.call('GET', KEYS[2]) == ARGV[4] then
    redis.call('DEL', KEYS[2])
    redis.call('SREM', KEYS[3], ARGV[3])
end

return 1
