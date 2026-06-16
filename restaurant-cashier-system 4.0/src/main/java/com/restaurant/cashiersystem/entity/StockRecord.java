package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("stock_records")
public class StockRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private Long materialId;
    private String type;        // "in" = 进货, "out" = 出库
    private BigDecimal quantity;
    private BigDecimal beforeStock;
    private BigDecimal afterStock;
    private Long operatorId;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
