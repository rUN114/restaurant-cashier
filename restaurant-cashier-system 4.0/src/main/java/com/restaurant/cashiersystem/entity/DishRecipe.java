package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dish_recipes")
public class DishRecipe {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private Long dishId;
    private Long materialId;
    private BigDecimal usedQuantity;

    @TableLogic
    private LocalDateTime deletedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}