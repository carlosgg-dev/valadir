-- rate_limit.lua
-- Sliding-window log rate limiter using a Redis sorted set.
-- Eliminates the fixed-window burst vulnerability (2x requests at window boundaries).
--
-- KEYS[1] = rate limit key  (e.g. rate_limit:ip:auth_login:1.2.3.4)
-- ARGV[1] = limit           (max requests allowed in the window)
-- ARGV[2] = window          (window duration in seconds)
-- ARGV[3] = now             (current epoch milliseconds — supplied by caller for determinism)
--
-- Returns: { current_count, retry_after_seconds }
--   retry_after_seconds — allowed: full window (for X-RateLimit-Reset); blocked: exact wait until oldest entry expires

local key     = KEYS[1]
local limit   = tonumber(ARGV[1])
local win_sec = tonumber(ARGV[2])
local now     = tonumber(ARGV[3])
local win_ms  = win_sec * 1000
local cutoff  = now - win_ms

-- 1. Evict timestamps that have left the sliding window
redis.call('ZREMRANGEBYSCORE', key, '-inf', cutoff)

-- 2. Count requests still inside the window (before adding this one)
local count = tonumber(redis.call('ZCARD', key))

-- 3. Record this request: unique member via a per-key sequence counter
local seq = redis.call('INCR', key .. ':seq')
redis.call('ZADD', key, now, seq)
redis.call('EXPIRE', key,           win_sec + 1)
redis.call('EXPIRE', key .. ':seq', win_sec + 1)

local new_count = count + 1

-- 4. Compute retry_after:
--    blocked  → exact seconds until the oldest entry exits the window
--    allowed  → full window (conservative upper bound for X-RateLimit-Reset header)
local retry_after
if new_count > limit then
    local oldest_score = tonumber(redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')[2])
    retry_after = math.ceil((oldest_score + win_ms - now) / 1000)
else
    retry_after = win_sec
end

return { new_count, math.max(1, retry_after) }
