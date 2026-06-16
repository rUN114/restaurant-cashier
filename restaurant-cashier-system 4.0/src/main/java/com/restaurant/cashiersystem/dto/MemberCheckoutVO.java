package com.restaurant.cashiersystem.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 收银台会员结算信息
 */
@Data
public class MemberCheckoutVO {
    /** 会员ID */
    private Long memberId;
    /** 会员姓名 */
    private String memberName;
    /** 会员手机号 */
    private String memberPhone;
    /** 当前等级名称 */
    private String levelName;
    /** 折扣率（9.50 = 9.5折） */
    private BigDecimal discountRate;
    /** 折扣金额 */
    private BigDecimal discountAmount;
    /** 可用积分 */
    private Integer availablePoints;
    /** 积分抵扣比例（多少积分抵1元） */
    private Integer pointsRedeemRate;
    /** 最大可抵扣积分 */
    private Integer maxRedeemPoints;
    /** 实际抵扣积分 */
    private Integer redeemPoints;
    /** 抵扣金额 */
    private BigDecimal redeemAmount;
    /** 预计获得积分 */
    private Integer expectedPoints;
    /** 折扣与积分互斥开关 */
    private Boolean discountPointsMutex;
    /** 计算后的最终实付金额 */
    private BigDecimal finalPayAmount;
}
