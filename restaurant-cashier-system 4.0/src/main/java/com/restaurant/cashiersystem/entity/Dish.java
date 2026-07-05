package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dishes")
public class Dish {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private Long categoryId;
    private String name;
    private BigDecimal price;
    private String unit;
    private String imageUrl;
    private String remark;
    private Integer isAvailable;
    /** 是否套餐 1=是 */
    private Integer isCombo;
    /** 套餐包含的菜品ID，逗号分隔 */
    private String comboDishes;
    /** 菜品标签，逗号分隔 如: 招牌,微辣,素食 */
    private String tags;

    // ---- 秒杀相关字段 ----
    /** 秒杀库存（独立于日常库存） */
    private Integer seckillStock;
    /** 秒杀价格 */
    private BigDecimal seckillPrice;
    /** 秒杀开始时间 */
    private LocalDateTime seckillStartTime;
    /** 秒杀结束时间 */
    private LocalDateTime seckillEndTime;

    @Version
    private Integer version;

    @TableLogic
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
