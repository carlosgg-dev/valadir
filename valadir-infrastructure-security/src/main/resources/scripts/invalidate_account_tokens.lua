-- Atomically invalidates every session of an account: the refresh tokens that let it renew, and
-- the access tokens already issued, cut off by their issue time.
-- Returns the number of refresh tokens revoked.
--
-- KEYS[1] = auth:user_tokens:{accountId}
-- KEYS[2] = auth:token_cutoff:{accountId}
-- ARGV[1] = auth:refresh_token: (prefix concatenated with each set member to rebuild its key)
-- ARGV[2] = cutoff in epoch seconds (every access token issued at or before it is rejected)
-- ARGV[3] = TTL in seconds for the cutoff (the access token lifetime: past it, nothing the cutoff
--           could reject is still alive)

local fingerprints = redis.call('SMEMBERS', KEYS[1])
for _, fingerprint in ipairs(fingerprints) do
    redis.call('DEL', ARGV[1] .. fingerprint)
end
redis.call('DEL', KEYS[1])
redis.call('SET', KEYS[2], ARGV[2], 'EX', ARGV[3])

return #fingerprints
