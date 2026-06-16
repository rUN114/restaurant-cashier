package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会员表
 */
@Data
@TableName("member")
public class Member {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 手机号（唯一标识） */
    private String phone;

    /** 会员姓名 */
    private String name;

    /** 性别 0未知 1男 2女 */
    private Integer gender;

    /** 生日 */
    private LocalDate birthday;

    /** 当前会员等级ID */
    private Long levelId;

    /** 累计获得积分 */
    private Integer totalPoints;

    /** 可用积分 */
    private Integer availablePoints;

    /** 累计消费金额 */
    private BigDecimal totalConsumption;

    /** 累计消费次数 */
    private Integer totalOrders;

    /** 最近消费时间 */
    private LocalDateTime lastConsumptionTime;

    /** 注册门店ID */
    private Long storeId;

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

    // ---- 非数据库字段 ----
    /** 关联的等级对象（联表查询时填充） */
    @TableField(exist = false)
    private MemberLevel level;
}
