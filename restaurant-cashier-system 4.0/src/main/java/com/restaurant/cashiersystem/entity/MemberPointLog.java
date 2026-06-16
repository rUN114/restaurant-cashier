package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员积分流水表
 */
@Data
@TableName("member_point_log")
public class MemberPointLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会员ID */
    private Long memberId;

    /** 关联订单ID */
    private Long orderId;

    /**
     * 变动类型
     * 1=消费加积分 2=抵现扣积分 3=退款扣积分
     * 4=后台手动增加 5=后台手动扣减 6=过期扣减
     */
    private Integer changeType;

    /** 变动积分数（正=增加，负=扣减） */
    private Integer changePoints;

    /** 变动前可用积分 */
    private Integer beforePoints;

    /** 变动后可用积分 */
    private Integer afterPoints;

    /** 关联金额 */
    private BigDecimal relatedAmount;

    /** 备注 */
    private String remark;

    /** 操作人ID */
    private Long operatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
