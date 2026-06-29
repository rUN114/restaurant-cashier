package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("materials")
public class Material {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private String name;
    /** 库存/配方单位（g, ml, 个） */
    private String unit;
    /** 采购单位（斤, kg, 瓶, 箱） */
    private String purchaseUnit;
    /** 1个采购单位 = 多少个库存单位（如 1斤=500g → 500） */
    private BigDecimal unitRate;
    private BigDecimal warningStock;

    @TableLogic
    private LocalDateTime deletedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}