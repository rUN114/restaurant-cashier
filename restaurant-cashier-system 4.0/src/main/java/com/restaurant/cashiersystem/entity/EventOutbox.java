package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件发件箱 — 保证 Redis 扣库存后订单创建不丢
 * <p>与订单创建在同一事务中写入，定时任务扫描投递</p>
 */
@Data
@TableName("event_outbox")
public class EventOutbox {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 事件类型: SECKILL_ORDER / ORDER_SETTLED */
    private String eventType;

    /** JSON 格式的事件负载 */
    private String payload;

    /** PENDING=待投递 SENT=已投递 FAILED=最终失败 */
    private String status;

    /** 已重试次数 */
    private Integer retryCount;

    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
