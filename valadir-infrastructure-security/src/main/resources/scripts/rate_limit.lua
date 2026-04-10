-- rate_limit.lua
-- Fixed-window counter with atomic increment and TTL.
--
-- KEYS[1] = rate limit key (e.g. rate_limit:ip:auth_login:1.2.3.4)
-- ARGV[1] = limit  (max requests allowed in the window)
-- ARGV[2] = window (duration in seconds)
--
-- Returns: { current_count, ttl_seconds }

local current = redis.call('INCR', KEYS[1])
if current == 1 then
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
end
local ttl = redis.call('TTL', KEYS[1])
return { current, ttl }
