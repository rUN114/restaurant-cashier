-- ============================================================
-- 秒杀功能数据库迁移脚本
-- 在 restaurant_chain 库执行
-- ============================================================

-- 1. event_outbox — 可靠消息投递表
DROP TABLE IF EXISTS `event_outbox`;
CREATE TABLE `event_outbox` (
    `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
    `event_type` varchar(64) NOT NULL COMMENT '事件类型 SECKILL_ORDER / ORDER_SETTLED',
    `payload` json NOT NULL COMMENT 'JSON 事件负载',
    `status` enum('PENDING','SENT','FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '投递状态',
    `retry_count` int NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `sent_at` datetime DEFAULT NULL COMMENT '成功投递时间',
    PRIMARY KEY (`id`),
    INDEX `idx_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='事件发件箱 — 保证异步消息不丢';

-- 2. dishes 表增加秒杀相关字段
ALTER TABLE `dishes`
    ADD COLUMN `seckill_stock` int UNSIGNED DEFAULT 0 COMMENT '秒杀库存（独立于正常库存）',
    ADD COLUMN `seckill_price` decimal(10,2) DEFAULT NULL COMMENT '秒杀价格',
    ADD COLUMN `seckill_start_time` datetime DEFAULT NULL COMMENT '秒杀开始时间',
    ADD COLUMN `seckill_end_time` datetime DEFAULT NULL COMMENT '秒杀结束时间';

-- 3. orders 表增加秒杀/幂等标记
ALTER TABLE `orders`
    ADD COLUMN `request_id` varchar(64) DEFAULT NULL COMMENT '幂等请求ID（秒杀/防重）',
    ADD COLUMN `order_type` varchar(32) DEFAULT 'NORMAL' COMMENT '订单类型 NORMAL / SECKILL',
    ADD UNIQUE INDEX `uk_request_id` (`request_id`);

-- 4. seckill_records — 秒杀记录表（Redis-MySQL 对账用）
DROP TABLE IF EXISTS `seckill_records`;
CREATE TABLE `seckill_records` (
    `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
    `dish_id` bigint UNSIGNED NOT NULL,
    `user_id` bigint UNSIGNED NOT NULL,
    `request_id` varchar(64) NOT NULL COMMENT '幂等请求ID',
    `status` enum('SUCCESS','ROLLBACK','PENDING') NOT NULL DEFAULT 'SUCCESS',
    `order_id` bigint UNSIGNED DEFAULT NULL COMMENT '关联订单ID',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_request_id` (`request_id`),
    INDEX `idx_dish_user` (`dish_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀记录 — 对账补偿用';
