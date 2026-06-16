package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("material_consumptions")
public class MaterialConsumption {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private Long orderId;
    private Long dishId;
    private Long materialId;
    private BigDecimal usedQuantity;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private LocalDateTime deletedAt;
}