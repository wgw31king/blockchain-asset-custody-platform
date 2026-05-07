-- Token bucket limiter (Redis HASH: tokens, ts). ARGV: capacity, refill_per_sec, now_seconds, cost
local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local cost = tonumber(ARGV[4])

local data = redis.call('HMGET', key, 'tokens', 'ts')
local tokens = tonumber(data[1])
local last = tonumber(data[2])
if tokens == nil then
    tokens = capacity
    last = now
end
local elapsed = math.max(0, now - last)
tokens = math.min(capacity, tokens + elapsed * refill)
if tokens < cost then
    redis.call('HSET', key, 'tokens', tokens, 'ts', now)
    redis.call('EXPIRE', key, 180)
    return 0
end
tokens = tokens - cost
redis.call('HSET', key, 'tokens', tokens, 'ts', now)
redis.call('EXPIRE', key, 180)
return 1
