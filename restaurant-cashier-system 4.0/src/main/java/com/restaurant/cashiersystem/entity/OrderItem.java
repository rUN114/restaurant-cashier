package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_items")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long storeId;
    private Long dishId;
    private String dishName;
    private BigDecimal price;
    private Integer quantity;
    private String status;
    private String remark;
    /** 退菜原因 */
    private String returnReason;
    /** 厨房状态: WAITING/COOKING/DONE/SERVED */
    private String kitchenStatus = "WAITING";
    /** 催单次数 */
    private Integer bumpCount;
    /** 最后催单时间 */
    private LocalDateTime bumpedAt;
    /** 开始制作时间 */
    private LocalDateTime cookingStartedAt;
    /** 制作完成时间 */
    private LocalDateTime doneAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Version
    private Integer version;

    @TableLogic
    private LocalDateTime deletedAt;
}
