package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.Member;
import com.restaurant.cashiersystem.entity.MemberLevel;
import com.restaurant.cashiersystem.mapper.MemberMapper;
import com.restaurant.cashiersystem.service.MemberLevelService;
import com.restaurant.cashiersystem.service.MemberPointLogService;
import com.restaurant.cashiersystem.service.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member>
        implements MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberServiceImpl.class);

    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private MemberLevelService memberLevelService;
    @Autowired
    private MemberPointLogService pointLogService;

    @Override
    @Transactional
    public Member registerOrFind(String phone, String name, Long storeId) {
        Member exist = memberMapper.selectByPhone(phone);
        if (exist != null) {
            if (exist.getStatus() != 1) {
                throw new RuntimeException("该会员已被禁用，请联系管理员");
            }
            return exist;
        }
        Member member = new Member();
        member.setPhone(phone);
        member.setName(name != null ? name : "会员" + phone.substring(7));
        member.setStoreId(storeId);
        member.setStatus(1);
        member.setTotalPoints(0);
        member.setAvailablePoints(0);
        member.setTotalConsumption(BigDecimal.ZERO);
        member.setTotalOrders(0);
        // 默认普通会员等级
        MemberLevel defaultLevel = memberLevelService.findUpgradeLevel(BigDecimal.ZERO);
        if (defaultLevel != null) {
            member.setLevelId(defaultLevel.getId());
        }
        save(member);
        log.info("新会员注册: phone={}, id={}", phone, member.getId());
        return member;
    }

    @Override
    public Member findByPhone(String phone) {
        return memberMapper.selectByPhone(phone);
    }

    @Override
    public Member findByIdForUpdate(Long id) {
        return memberMapper.selectByIdForUpdate(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addConsumptionAndPoints(Long memberId, BigDecimal amount, int points) {
        // 加锁查询，防并发
        Member member = memberMapper.selectByIdForUpdate(memberId);
        if (member == null || member.getStatus() != 1) {
            throw new RuntimeException("会员不存在或已禁用");
        }

        int beforePoints = member.getAvailablePoints();

        // 增加累计消费
        BigDecimal newTotal = member.getTotalConsumption().add(amount);
        memberMapper.addConsumption(memberId, amount);

        // 增加积分
        if (points > 0) {
            memberMapper.addPoints(memberId, points);
        }

        // 积分流水
        pointLogService.log(memberId, null, 1,
                points, beforePoints, beforePoints + points,
                amount, "消费获得积分", null);

        // 自动升级检查
        checkAndUpgrade(memberId, newTotal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addPoints(Long memberId, int points) {
        if (points <= 0) return 0;

        Member member = memberMapper.selectByIdForUpdate(memberId);
        if (member == null || member.getStatus() != 1) {
            throw new RuntimeException("会员不存在或已禁用");
        }

        int beforePoints = member.getAvailablePoints();
        int result = memberMapper.addPoints(memberId, points);
        if (result > 0) {
            pointLogService.log(memberId, null, 4,
                    points, beforePoints, beforePoints + points,
                    null, "后台手动增加积分", null);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductPoints(Long memberId, int points) {
        if (points <= 0) return false;

        Member member = memberMapper.selectByIdForUpdate(memberId);
        if (member == null || member.getStatus() != 1) {
            throw new RuntimeException("会员不存在或已禁用");
        }
        if (member.getAvailablePoints() < points) {
            return false;
        }

        int beforePoints = member.getAvailablePoints();
        int result = memberMapper.deductPoints(memberId, points);
        if (result > 0) {
            pointLogService.log(memberId, null, 2,
                    -points, beforePoints, beforePoints - points,
                    null, "积分抵现", null);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundRollback(Long memberId, BigDecimal refundAmount, int refundPoints) {
        Member member = memberMapper.selectByIdForUpdate(memberId);
        if (member == null) {
            log.warn("退款回滚：会员不存在 id={}", memberId);
            return;
        }

        int beforePoints = member.getAvailablePoints();

        // 回退积分
        if (refundPoints > 0) {
            memberMapper.addPoints(memberId, -refundPoints); // 负数=扣减
            pointLogService.log(memberId, null, 3,
                    -refundPoints, beforePoints, beforePoints - refundPoints,
                    refundAmount, "退款扣除积分", null);
        }

        // 回退累计消费（使用负数加法，确保不低于0）
        BigDecimal newTotal = member.getTotalConsumption()
                .subtract(refundAmount).max(BigDecimal.ZERO);
        Member updateMember = new Member();
        updateMember.setId(memberId);
        updateMember.setTotalConsumption(newTotal);
        updateById(updateMember);

        log.info("退款回滚完成: memberId={}, refundAmount={}, refundPoints={}",
                memberId, refundAmount, refundPoints);
    }

    // ---- 内部方法 ----

    /**
     * 检查并自动升级会员等级（只升不降）
     */
    private void checkAndUpgrade(Long memberId, BigDecimal totalConsumption) {
        Member member = getById(memberId);
        if (member == null) return;

        MemberLevel targetLevel = memberLevelService.findUpgradeLevel(totalConsumption);
        if (targetLevel == null) return;

        MemberLevel currentLevel = memberLevelService.getById(member.getLevelId());
        if (currentLevel == null) return;

        // 只升不降
        if (targetLevel.getSortOrder() > currentLevel.getSortOrder()) {
            memberMapper.updateLevel(memberId, targetLevel.getId());
            log.info("会员自动升级: memberId={}, phone={}, {} -> {}",
                    memberId, member.getPhone(), currentLevel.getName(), targetLevel.getName());
        }
    }
}
