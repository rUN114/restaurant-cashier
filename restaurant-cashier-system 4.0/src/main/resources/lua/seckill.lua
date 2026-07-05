-- seckill.lua — 秒杀原子操作
-- KEYS[1]: seckill:stock:{dishId}    库存计数器
-- KEYS[2]: seckill:user:{dishId}     已抢用户集合
-- KEYS[3]: seckill:result:{requestId} 请求结果标记
-- ARGV[1]: userId
-- ARGV[2]: requestId
--
-- 返回值:
--   1  抢购成功
--   0  库存不足
--  -1  用户已抢过
--  -2  幂等：requestId 已处理（直接返回上次结果）

local stock_key  = KEYS[1]
local user_key   = KEYS[2]
local result_key = KEYS[3]
local user_id    = ARGV[1]
local request_id = ARGV[2]

-- 幂等：同一个 requestId 已经处理过，直接返回之前的结果
local existing = redis.call('GET', result_key)
if existing then
    return tonumber(existing)
end

-- 检查用户是否已抢过该菜品
if redis.call('SISMEMBER', user_key, user_id) == 1 then
    redis.call('SETEX', result_key, 300, '-1')
    return -1
end

-- 检查并扣减库存
local stock = tonumber(redis.call('GET', stock_key) or '0')
if stock <= 0 then
    redis.call('SETEX', result_key, 300, '0')
    return 0
end

-- 原子扣库存 + 记录用户 + 写结果标记
redis.call('DECR', stock_key)
redis.call('SADD', user_key, user_id)
redis.call('SETEX', result_key, 300, '1')
return 1
