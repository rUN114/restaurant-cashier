package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀记录 — 用于 Redis-MySQL 对账
 */
@Data
@TableName("seckill_records")
public class SeckillRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dishId;
    private Long userId;
    private String requestId;
    private String status;    // SUCCESS / ROLLBACK / PENDING
    private Long orderId;     // 关联订单ID
    private LocalDateTime createdAt;
}
