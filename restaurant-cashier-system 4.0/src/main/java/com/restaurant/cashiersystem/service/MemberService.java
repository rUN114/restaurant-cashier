package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.cashiersystem.entity.Member;

public interface MemberService extends IService<Member> {

    /** 手机号注册/查找会员 */
    Member registerOrFind(String phone, String name, Long storeId);

    /** 根据手机号查找 */
    Member findByPhone(String phone);

    /** 加锁查询 */
    Member findByIdForUpdate(Long id);

    /** 增加累计消费 + 积分 */
    void addConsumptionAndPoints(Long memberId, java.math.BigDecimal amount, int points);

    /** 增加积分（正数=增加） */
    int addPoints(Long memberId, int points);

    /** 扣减积分 */
    boolean deductPoints(Long memberId, int points);

    /** 退款回滚（扣回积分） */
    void refundRollback(Long memberId, java.math.BigDecimal refundAmount, int refundPoints);
}
