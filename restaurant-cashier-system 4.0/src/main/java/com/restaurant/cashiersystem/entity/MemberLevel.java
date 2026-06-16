package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员等级配置表
 */
@Data
@TableName("member_level")
public class MemberLevel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 等级名称 */
    private String name;

    /** 排序（数值越大等级越高） */
    private Integer sortOrder;

    /** 升级所需累计消费金额 */
    private BigDecimal minConsumption;

    /** 折扣率（9.50=9.5折，10.00=不打折） */
    private BigDecimal discountRate;

    /** 图标URL */
    private String icon;

    /** 等级标识颜色 */
    private String color;

    /** 备注 */
    private String remark;

    /** 状态 1启用 0禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除 */
    @TableLogic
    private LocalDateTime deletedAt;
}
