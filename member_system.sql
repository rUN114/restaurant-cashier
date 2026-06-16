-- ============================================
-- 会员体系完整建表SQL
-- 兼容: MySQL 8.0+, utf8mb4
-- 项目: 餐饮收银系统 v2.1
-- ============================================

-- 1. 会员等级配置表
DROP TABLE IF EXISTS `member_level`;
CREATE TABLE `member_level` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(32) NOT NULL COMMENT '等级名称（普通/银卡/金卡/钻石）',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序（数值越大等级越高）',
    `min_consumption` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '升级所需累计消费金额',
    `discount_rate` DECIMAL(4,2) NOT NULL DEFAULT 10.00 COMMENT '折扣率（9.50=9.5折，10.00=不打折）',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '等级图标URL',
    `color` VARCHAR(16) DEFAULT '#999' COMMENT '等级标识颜色',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_sort` (`sort_order`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员等级配置表';

-- 2. 会员表
DROP TABLE IF EXISTS `member`;
CREATE TABLE `member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号（唯一标识）',
    `name` VARCHAR(32) DEFAULT NULL COMMENT '会员姓名',
    `gender` TINYINT DEFAULT 0 COMMENT '性别 0未知 1男 2女',
    `birthday` DATE DEFAULT NULL COMMENT '生日',
    `level_id` BIGINT DEFAULT NULL COMMENT '当前会员等级ID',
    `total_points` INT NOT NULL DEFAULT 0 COMMENT '累计获得积分',
    `available_points` INT NOT NULL DEFAULT 0 COMMENT '可用积分',
    `total_consumption` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费金额',
    `total_orders` INT NOT NULL DEFAULT 0 COMMENT '累计消费次数',
    `last_consumption_time` DATETIME DEFAULT NULL COMMENT '最近消费时间',
    `store_id` BIGINT DEFAULT NULL COMMENT '注册门店ID',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_level` (`level_id`),
    KEY `idx_store` (`store_id`),
    KEY `idx_phone` (`phone`),
    KEY `idx_status` (`status`),
    KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员表';

-- 3. 积分流水表
DROP TABLE IF EXISTS `member_point_log`;
CREATE TABLE `member_point_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `member_id` BIGINT NOT NULL COMMENT '会员ID',
    `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID',
    `change_type` TINYINT NOT NULL COMMENT '变动类型 1消费加积分 2抵现扣积分 3退款扣积分 4后台手动增加 5后台手动扣减 6过期扣减',
    `change_points` INT NOT NULL COMMENT '变动积分数（正数=增加，负数=扣减）',
    `before_points` INT NOT NULL DEFAULT 0 COMMENT '变动前可用积分',
    `after_points` INT NOT NULL DEFAULT 0 COMMENT '变动后可用积分',
    `related_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '关联金额',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注说明',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_member` (`member_id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_type` (`change_type`),
    KEY `idx_created` (`created_at`),
    KEY `idx_member_created` (`member_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员积分流水表';

-- ============================================
-- 初始化数据
-- ============================================

-- 会员等级初始化（4档）
INSERT INTO `member_level` (`id`, `name`, `sort_order`, `min_consumption`, `discount_rate`, `color`, `remark`) VALUES
(1, '普通会员', 1, 0.00, 10.00, '#999', '注册即享，无折扣'),
(2, '银卡会员', 2, 2000.00, 9.50, '#888', '累计消费满2000升级，享9.5折'),
(3, '金卡会员', 3, 5000.00, 9.00, '#FFD700', '累计消费满5000升级，享9折'),
(4, '钻石会员', 4, 10000.00, 8.50, '#00BFFF', '累计消费满10000升级，享8.5折');

-- 积分规则初始化（插入到 sys_config 表，如果不存在则创建）
-- 方式：直接 INSERT IGNORE 或检查后插入
INSERT IGNORE INTO `sys_config` (`config_key`, `config_value`, `config_group`, `remark`, `status`) VALUES
('member.points_per_yuan', '1', '会员配置', '每消费1元获得的积分数', 1),
('member.points_redeem_rate', '100', '会员配置', '多少积分可抵扣1元', 1),
('member.points_redeem_max_percent', '30', '会员配置', '单笔订单积分抵扣最大百分比', 1),
('member.discount_points_mutex', 'true', '会员配置', '会员折扣与积分抵现互斥开关', 1),
('member.auto_upgrade', 'true', '会员配置', '是否自动升级会员等级', 1),
('member.points_expire_days', '365', '会员配置', '积分有效期（天），0=永不过期', 1);

-- 测试会员数据（可选）
INSERT IGNORE INTO `member` (`id`, `phone`, `name`, `level_id`, `total_points`, `available_points`, `total_consumption`, `store_id`) VALUES
(1, '13800138000', '测试会员', 1, 500, 500, 1500.00, NULL);
