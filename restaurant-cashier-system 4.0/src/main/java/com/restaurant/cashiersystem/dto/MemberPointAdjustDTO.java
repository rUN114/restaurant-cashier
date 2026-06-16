package com.restaurant.cashiersystem.dto;

import lombok.Data;

/**
 * 手动调整积分请求
 */
@Data
public class MemberPointAdjustDTO {
    private Long memberId;
    private Integer points;      // 正数=增加，负数=扣减
    private String remark;
}
