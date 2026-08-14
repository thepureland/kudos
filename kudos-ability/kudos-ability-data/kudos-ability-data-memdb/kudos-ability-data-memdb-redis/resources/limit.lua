-- 固定窗口限流：窗口内最多放行 limit 次
-- KEYS[1] 计数器 key；ARGV[1] 限流阈值；ARGV[2] 窗口秒数
-- 返回 0 表示拒绝；返回正数表示放行（值为窗口内已放行的次数）
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local ttl = tonumber(ARGV[2])
-- 当前计数（key 不存在视为 0）
local current = tonumber(redis.call('GET', key) or '0')
if current + 1 > limit then
    -- 拒绝；不刷新过期时间，窗口从首次放行的请求起固定计时
    return 0
end
current = redis.call('INCR', key)
-- 仅窗口首次放行时设置过期；TTL<0 的保底分支防止历史遗留的无过期 key 永不释放
if current == 1 or redis.call('TTL', key) < 0 then
    redis.call('EXPIRE', key, ttl)
end
return current
