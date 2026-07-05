package com.restaurant.cashiersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 秒杀异步消息 — Redis 扣库存成功后投递到 MQ，异步创建订单
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long dishId;
    private Long userId;
    private String requestId;
}
