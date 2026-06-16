package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.MemberPointLog;
import com.restaurant.cashiersystem.mapper.MemberPointLogMapper;
import com.restaurant.cashiersystem.service.MemberPointLogService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MemberPointLogServiceImpl extends ServiceImpl<MemberPointLogMapper, MemberPointLog>
        implements MemberPointLogService {

    @Override
    public void log(Long memberId, Long orderId, int changeType,
                    int changePoints, int beforePoints, int afterPoints,
                    BigDecimal relatedAmount, String remark, Long operatorId) {
        MemberPointLog log = new MemberPointLog();
        log.setMemberId(memberId);
        log.setOrderId(orderId);
        log.setChangeType(changeType);
        log.setChangePoints(changePoints);
        log.setBeforePoints(beforePoints);
        log.setAfterPoints(afterPoints);
        log.setRelatedAmount(relatedAmount);
        log.setRemark(remark);
        log.setOperatorId(operatorId);
        save(log);
    }
}
