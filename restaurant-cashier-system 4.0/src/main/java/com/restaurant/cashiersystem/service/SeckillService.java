package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.entity.Dish;
import com.restaurant.cashiersystem.entity.Order;
import com.restaurant.cashiersystem.entity.SeckillRecord;
import com.restaurant.cashiersystem.mapper.DishMapper;
import com.restaurant.cashiersystem.mapper.OrderMapper;
import com.restaurant.cashiersystem.mapper.SeckillRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * 秒杀服务 — Lua 原子扣库存 + PENDING 补偿 + Outbox 可靠投递
 */
@Service
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    /** Redis Key 前缀 */
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String USER_KEY_PREFIX = "seckill:user:";
    private static final String RESULT_KEY_PREFIX = "seckill:result:";

    /** 结果 TTL：5 分钟 */
    private static final Duration RESULT_TTL = Duration.ofMinutes(5);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SeckillRecordMapper seckillRecordMapper;

    /** Lua 脚本 */
    private final DefaultRedisScript<Long> seckillScript;

    public SeckillService() {
        seckillScript = new DefaultRedisScript<>();
        seckillScript.setLocation(
            new org.springframework.core.io.ClassPathResource("lua/seckill.lua"));
        seckillScript.setResultType(Long.class);
    }

    /**
     * 查询秒杀结果 — 客户端超时/丢包后通过此接口确认
     * <p>requestId 全局唯一（uk_request_id），无需 dishId/userId 辅助定位</p>
     */
    public String queryResult(String requestId) {
        try {
            String key = RESULT_KEY_PREFIX + requestId;
            String result = stringRedisTemplate.opsForValue().get(key);
            if (result != null) {
                return mapCode(Integer.parseInt(result));
            }

            // Redis 无记录，回查 MySQL
            LambdaQueryWrapper<SeckillRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SeckillRecord::getRequestId, requestId);
            SeckillRecord record = seckillRecordMapper.selectOne(wrapper);
            if (record != null) {
                return record.getStatus();
            }

            return "UNKNOWN";
        } catch (Exception e) {
            log.warn("查询秒杀结果异常: requestId={}", requestId, e);
            return "UNKNOWN";
        }
    }

    /**
     * 执行秒杀 — Redis Lua 原子扣库存
     *
     * @return 1=成功 0=库存不足 -1=已抢过 -2=幂等
     */
    public int trySeckill(Long dishId, Long userId, String requestId) {
        String stockKey = STOCK_KEY_PREFIX + dishId;
        String userKey = USER_KEY_PREFIX + dishId;
        String resultKey = RESULT_KEY_PREFIX + requestId;

        String userIdStr = userId.toString();

        try {
            Long code = stringRedisTemplate.execute(
                    seckillScript,
                    List.of(stockKey, userKey, resultKey),
                    userIdStr, requestId);

            if (code == null) {
                log.error("Lua 脚本返回 null: dishId={}, userId={}, requestId={}", dishId, userId, requestId);
                return -3; // 脚本执行异常
            }
            return code.intValue();
        } catch (DataAccessException e) {
            // ── Redis 超时：不知道 Lua 到底执没执行 ──
            log.error("Redis 超时, 标记 PENDING: dishId={}, userId={}, requestId={}", dishId, userId, requestId, e);

            // 尝试写 PENDING 标记（code=2），如果之前已写入则保留原值
            try {
                stringRedisTemplate.opsForValue()
                        .setIfAbsent(RESULT_KEY_PREFIX + requestId, "2", RESULT_TTL);
            } catch (Exception ignored) {
                // 写标记失败不阻塞返回
            }

            // 写一条 PENDING 状态的秒杀记录到 MySQL（用于后续对账）
            try {
                savePendingRecord(dishId, userId, requestId);
            } catch (Exception ignored) {
                log.warn("写 PENDING 秒杀记录失败: requestId={}", requestId);
            }

            return -3; // 网络异常
        }
    }

    /**
     * 异步落库 — 创建秒杀订单 + Outbox（同一事务）
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createSeckillOrder(Long dishId, Long userId, String requestId) {
        // 1. 幂等检查：同一个 requestId 是否已创建订单
        LambdaQueryWrapper<SeckillRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(SeckillRecord::getRequestId, requestId);
        SeckillRecord existingRecord = seckillRecordMapper.selectOne(recordWrapper);
        if (existingRecord != null && existingRecord.getOrderId() != null) {
            return orderMapper.selectById(existingRecord.getOrderId());
        }

        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw new IllegalArgumentException("菜品不存在");
        }

        // 2. 乐观锁扣 MySQL 库存
        int rows = dishMapper.deductSeckillStock(dishId, dish.getVersion());
        if (rows == 0) {
            // 库存已空，回滚 Redis
            rollbackRedis(dishId, userId);
            throw new RuntimeException("已被抢光");
        }

        // 3. 创建订单（唯一索引 uk_request_id 兜底竞态）
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setStoreId(dish.getStoreId());
        order.setStatus("settled");
        order.setTotalAmount(dish.getSeckillPrice() != null ? dish.getSeckillPrice() : BigDecimal.ONE);
        order.setFinalAmount(dish.getSeckillPrice() != null ? dish.getSeckillPrice() : BigDecimal.ONE);
        order.setPaymentMethod("seckill");
        order.setSettledBy(userId);
        order.setSettledAt(LocalDateTime.now());
        order.setCreatedBy(userId);
        order.setCreatedAt(LocalDateTime.now());
        order.setOrderType("SECKILL");
        order.setRequestId(requestId);
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // TOCTOU 竞态：另一个线程已为同一 requestId 创建了订单
            log.warn("requestId 重复，查询已有订单: requestId={}", requestId);
            LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(Order::getRequestId, requestId);
            Order existingOrder = orderMapper.selectOne(orderWrapper);
            if (existingOrder != null) {
                return existingOrder;
            }
            // 理论上不会到这里，但万一到了就当异常抛出
            throw new RuntimeException("订单创建冲突，请重试", e);
        }

        // 4. 更新秒杀记录
        if (existingRecord != null) {
            existingRecord.setStatus("SUCCESS");
            existingRecord.setOrderId(order.getId());
            seckillRecordMapper.updateById(existingRecord);
        } else {
            SeckillRecord record = new SeckillRecord();
            record.setDishId(dishId);
            record.setUserId(userId);
            record.setRequestId(requestId);
            record.setStatus("SUCCESS");
            record.setOrderId(order.getId());
            try {
                seckillRecordMapper.insert(record);
            } catch (DuplicateKeyException e) {
                // requestId 已存在（另一线程先插入了），更新而非插入
                log.warn("秒杀记录已存在，改为更新: requestId={}", requestId);
                SeckillRecord dup = seckillRecordMapper.selectOne(recordWrapper);
                if (dup != null) {
                    dup.setStatus("SUCCESS");
                    dup.setOrderId(order.getId());
                    seckillRecordMapper.updateById(dup);
                }
            }
        }

        log.info("秒杀订单创建成功: dishId={}, userId={}, orderId={}, requestId={}",
                dishId, userId, order.getId(), requestId);
        return order;
    }

    /**
     * 回滚 Redis 库存（异步落库失败时）
     */
    public void rollbackRedis(Long dishId, Long userId) {
        try {
            stringRedisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + dishId);
            stringRedisTemplate.opsForSet().remove(USER_KEY_PREFIX + dishId, userId.toString());
            log.info("已回滚 Redis 秒杀库存: dishId={}, userId={}", dishId, userId);
        } catch (Exception e) {
            log.error("回滚 Redis 秒杀库存失败: dishId={}, userId={}", dishId, userId, e);
        }
    }

    /**
     * 获取 Redis 中该菜品的已抢用户集合（对账用）
     * <p>使用 SSCAN 分批读取，避免大 Key 阻塞 Redis</p>
     */
    public Set<String> getRedisWinners(Long dishId) {
        try {
            String key = USER_KEY_PREFIX + dishId;
            Set<String> result = new java.util.HashSet<>();
            ScanOptions options = ScanOptions.scanOptions()
                    .match("*")
                    .count(1000)
                    .build();
            try (Cursor<String> cursor = stringRedisTemplate.opsForSet().scan(key, options)) {
                while (cursor.hasNext()) {
                    result.add(cursor.next());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("获取 Redis 秒杀用户集合失败: dishId={}", dishId, e);
            return Set.of();
        }
    }

    /**
     * 检查用户是否有 PENDING 秒杀记录（用于 -1 时的回查）
     * @return requestId（有 PENDING）或 null（无）
     */
    public String checkUserHasPending(Long dishId, Long userId) {
        try {
            LambdaQueryWrapper<SeckillRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SeckillRecord::getDishId, dishId)
                   .eq(SeckillRecord::getUserId, userId)
                   .eq(SeckillRecord::getStatus, "PENDING")
                   .last("LIMIT 1");
            SeckillRecord record = seckillRecordMapper.selectOne(wrapper);
            return record != null ? record.getRequestId() : null;
        } catch (Exception e) {
            log.warn("查询 PENDING 记录失败: dishId={}, userId={}", dishId, userId, e);
            return null;
        }
    }

    /**
     * 预热秒杀库存 — 从 MySQL dishes.seckill_stock 加载到 Redis
     * @return 加载的库存数量
     */
    public int preloadStock(Long dishId) {
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw new IllegalArgumentException("菜品不存在");
        }
        if (dish.getSeckillStock() == null || dish.getSeckillStock() <= 0) {
            throw new IllegalArgumentException("该菜品未设置秒杀库存");
        }
        String stockKey = STOCK_KEY_PREFIX + dishId;
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(dish.getSeckillStock()));
        log.info("秒杀库存预热完成: dishId={}, stock={}", dishId, dish.getSeckillStock());
        return dish.getSeckillStock();
    }

    /**
     * 获取秒杀当前库存（监控用）
     */
    public int getCurrentStock(Long dishId) {
        try {
            String val = stringRedisTemplate.opsForValue().get(STOCK_KEY_PREFIX + dishId);
            return val != null ? Integer.parseInt(val) : 0;
        } catch (Exception e) {
            log.warn("获取秒杀库存失败: dishId={}", dishId, e);
            return -1;
        }
    }

    // ==================== 内部方法 ====================

    private void savePendingRecord(Long dishId, Long userId, String requestId) {
        SeckillRecord record = new SeckillRecord();
        record.setDishId(dishId);
        record.setUserId(userId);
        record.setRequestId(requestId);
        record.setStatus("PENDING");
        try {
            seckillRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发重试导致同一 requestId 被多次标记 PENDING，第一条已生效，忽略即可
            log.warn("PENDING 记录重复（requestId={}），已忽略", requestId);
        }
    }

    private String generateOrderNo() {
        return "SEC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + String.format("%05d", new java.security.SecureRandom().nextInt(100000));
    }

    private String mapCode(int code) {
        return switch (code) {
            case 1 -> "SUCCESS";
            case 0 -> "SOLD_OUT";
            case -1 -> "ALREADY_GRABBED";
            case 2 -> "PENDING";
            default -> "UNKNOWN";
        };
    }
}
