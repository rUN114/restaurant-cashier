package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_item_logs")
public class OrderItemLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderItemId;
    private Long orderId;
    private Long storeId;
    private Long dishId;
    private String actionType;
    private Integer changeQuantity;
    private String returnReason;
    private Long operatorId;
    private LocalDateTime createdAt;
}