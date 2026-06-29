package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("material_inventory")
public class MaterialInventory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private Long materialId;
    private BigDecimal currentStock;
    private BigDecimal costPrice;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}