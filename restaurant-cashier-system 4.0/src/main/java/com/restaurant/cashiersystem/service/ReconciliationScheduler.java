package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.entity.Order;
import com.restaurant.cashiersystem.entity.SeckillRecord;
import com.restaurant.cashiersystem.mapper.OrderMapper;
import com.restaurant.cashiersystem.mapper.SeckillRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 秒杀对账调度器
 * <ul>
 *   <li>每 5 分钟扫描 MySQL PENDING 记录，回查 Redis 确认结果并补偿</li>
 *   <li>每小时 Redis vs MySQL 全额对账，补偿漏单</li>
 * </ul>
 */
@Service
public class ReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationScheduler.class);

    @Autowired
    private SeckillRecordMapper seckillRecordMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SeckillService seckillService;

    /**
     * 每 5 分钟：处理 PENDING 状态的秒杀记录（Redis 超时导致的）
     * 回查 Redis 结果缓存 → 有结果则补偿创建订单，无结果则保持 PENDING
     */
    @Scheduled(fixedDelay = 300_000) // 5 分钟
    public void resolvePendingRecords() {
        log.info("开始处理 PENDING 秒杀记录...");

        LambdaQueryWrapper<SeckillRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillRecord::getStatus, "PENDING")
               .last("LIMIT 500");
        List<SeckillRecord> pending = seckillRecordMapper.selectList(wrapper);

        if (pending.isEmpty()) {
            log.debug("无 PENDING 秒杀记录待处理");
            return;
        }

        int resolved = 0;
        int remaining = 0;
        for (SeckillRecord record : pending) {
            String result = seckillService.queryResult(record.getRequestId());

            switch (result) {
                case "SUCCESS":
                    // Redis 确认成功但订单没创建 → 补偿
                    try {
                        Order order = seckillService.createSeckillOrder(
                                record.getDishId(), record.getUserId(), record.getRequestId());
                        log.info("PENDING 补偿成功: requestId={}, orderId={}",
                                record.getRequestId(), order.getId());
                        resolved++;
                    } catch (RuntimeException e) {
                        // 可能库存已空，回滚 Redis 并标记失败
                        log.warn("PENDING 补偿失败(库存不足), 标记 ROLLBACK: requestId={}", record.getRequestId());
                        record.setStatus("ROLLBACK");
                        seckillRecordMapper.updateById(record);
                        seckillService.rollbackRedis(record.getDishId(), record.getUserId());
                        resolved++;
                    }
                    break;

                case "SOLD_OUT":
                case "ALREADY_GRABBED":
                    // Redis 确认失败 → 标记回滚 + 从 Redis set 移除，防止对账重复补偿
                    record.setStatus("ROLLBACK");
                    seckillRecordMapper.updateById(record);
                    seckillService.rollbackRedis(record.getDishId(), record.getUserId());
                    resolved++;
                    break;

                case "UNKNOWN":
                default:
                    // 仍然不确定 → 保持 PENDING，等下次处理
                    remaining++;
                    break;
            }
        }

        log.info("PENDING 处理完成: 总量={}, 已解决={}, 仍待定={}", pending.size(), resolved, remaining);
    }

    /**
     * 每小时：Redis vs MySQL 全额对账
     * 找出 Redis 中标了但 MySQL 没订单的差集，补偿创建
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void fullReconciliation() {
        log.info("开始 Redis-MySQL 全额对账...");

        // 扫描 MySQL 中所有非 PENDING 的秒杀记录，按 dishId 分组
        LambdaQueryWrapper<SeckillRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(SeckillRecord::getStatus, "PENDING")
               .isNotNull(SeckillRecord::getOrderId)
               .last("LIMIT 10000");
        List<SeckillRecord> successRecords = seckillRecordMapper.selectList(wrapper);

        if (successRecords.isEmpty()) {
            log.debug("无秒杀记录需要对账");
            return;
        }

        // 按菜品分组对账
        var byDish = successRecords.stream()
                .collect(Collectors.groupingBy(SeckillRecord::getDishId));

        int totalCompensated = 0;
        for (var entry : byDish.entrySet()) {
            Long dishId = entry.getKey();
            List<SeckillRecord> records = entry.getValue();

            // MySQL 中该菜品的秒杀成功用户集合
            Set<String> dbWinners = records.stream()
                    .map(r -> r.getUserId().toString())
                    .collect(Collectors.toSet());

            // Redis 中该菜品的已抢用户集合
            Set<String> redisWinners = seckillService.getRedisWinners(dishId);
            if (redisWinners == null || redisWinners.isEmpty()) {
                continue;
            }

            // 差集 = Redis 有但 MySQL 没有的 userId
            Set<String> missing = new HashSet<>(redisWinners);
            missing.removeAll(dbWinners);

            if (!missing.isEmpty()) {
                log.warn("对账发现漏单: dishId={}, missingUsers={}", dishId, missing);
                for (String userIdStr : missing) {
                    try {
                        Long userId = Long.valueOf(userIdStr);
                        // 检查是否已有 PENDING 记录
                        LambdaQueryWrapper<SeckillRecord> pendingCheck = new LambdaQueryWrapper<>();
                        pendingCheck.eq(SeckillRecord::getDishId, dishId)
                                    .eq(SeckillRecord::getUserId, userId)
                                    .eq(SeckillRecord::getStatus, "PENDING");
                        SeckillRecord existingPending = seckillRecordMapper.selectOne(pendingCheck);

                        String requestId = existingPending != null
                                ? existingPending.getRequestId()
                                : "RECONCILE-" + dishId + "-" + userId + "-" + System.currentTimeMillis();

                        Order order = seckillService.createSeckillOrder(dishId, userId, requestId);
                        log.info("对账补偿创建订单: userId={}, orderId={}, requestId={}",
                                userId, order.getId(), requestId);
                        totalCompensated++;
                    } catch (Exception e) {
                        log.error("对账补偿失败: dishId={}, userId={}", dishId, userIdStr, e);
                    }
                }
            }
        }

        log.info("全额对账完成: 检查记录数={}, 补偿订单={}", successRecords.size(), totalCompensated);
    }
}
