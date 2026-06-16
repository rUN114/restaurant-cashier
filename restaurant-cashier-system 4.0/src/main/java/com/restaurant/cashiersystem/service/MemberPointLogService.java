package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.cashiersystem.entity.MemberPointLog;

public interface MemberPointLogService extends IService<MemberPointLog> {

    /** 记录积分变动 */
    void log(Long memberId, Long orderId, int changeType,
             int changePoints, int beforePoints, int afterPoints,
             java.math.BigDecimal relatedAmount, String remark, Long operatorId);
}
