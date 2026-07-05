package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.dto.MemberCheckoutVO;
import com.restaurant.cashiersystem.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 会员结算核心服务
 * 处理：折扣计算、积分抵现、互斥规则、积分预估
 */
@Service
public class MemberCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(MemberCheckoutService.class);

    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberLevelService memberLevelService;
    @Autowired
    private SysConfigService sysConfigService;

    /** 获取配置：每消费1元获得的积分数 */
    private int getPointsPerYuan() {
        return sysConfigService.getIntValue("member.points_per_yuan", 1);
    }

    /** 获取配置：多少积分可抵扣1元 */
    private int getPointsRedeemRate() {
        return sysConfigService.getIntValue("member.points_redeem_rate", 100);
    }

    /** 获取配置：单笔订单积分抵扣最大百分比 */
    private BigDecimal getRedeemMaxPercent() {
        int pct = sysConfigService.getIntValue("member.points_redeem_max_percent", 30);
        return BigDecimal.valueOf(pct).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    /** 获取配置：折扣与积分互斥开关 */
    private boolean isDiscountPointsMutex() {
        return sysConfigService.getBoolValue("member.discount_points_mutex", true);
    }

    /**
     * 计算会员结算信息
     *
     * @param memberId   会员ID
     * @param orderTotal 订单原价
     * @param useDiscount 是否使用折扣
     * @param redeemPoints 用户选择抵扣的积分（0=不抵扣）
     */
    public MemberCheckoutVO calculate(Long memberId, BigDecimal orderTotal,
                                       boolean useDiscount, int redeemPoints) {
        if (memberId == null || orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Member member = memberService.getById(memberId);
        if (member == null || member.getStatus() != 1) {
            log.warn("会员不可用: id={}", memberId);
            return null;
        }

        MemberLevel level = memberLevelService.getById(member.getLevelId());
        if (level == null || level.getStatus() != 1) {
            level = memberLevelService.findUpgradeLevel(BigDecimal.ZERO);
        }
        // 极端情况：没有任何等级配置，给个默认值防止 NPE
        if (level == null) {
            level = new MemberLevel();
            level.setName("普通会员");
            level.setDiscountRate(BigDecimal.TEN);
        }

        MemberCheckoutVO vo = new MemberCheckoutVO();
        vo.setMemberId(member.getId());
        vo.setMemberName(member.getName());
        vo.setMemberPhone(member.getPhone());
        vo.setLevelName(level.getName());
        vo.setDiscountRate(level.getDiscountRate());
        vo.setAvailablePoints(member.getAvailablePoints());
        vo.setPointsRedeemRate(getPointsRedeemRate());
        vo.setDiscountPointsMutex(isDiscountPointsMutex());

        BigDecimal finalAmount = orderTotal;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal redeemAmount = BigDecimal.ZERO;
        int actualRedeemPoints = 0;

        // 互斥规则：折扣和积分抵现不能同时使用
        if (isDiscountPointsMutex()) {
            if (useDiscount && redeemPoints > 0) {
                // 二选一，优先折扣（可配置改为优先抵现）
                log.info("折扣与抵现互斥，优先使用折扣");
                redeemPoints = 0;
            }
        }

        // 计算折扣
        if (useDiscount && level.getDiscountRate().compareTo(BigDecimal.TEN) < 0) {
            BigDecimal rate = level.getDiscountRate().divide(BigDecimal.TEN, 4, RoundingMode.HALF_UP);
            discountAmount = orderTotal.multiply(BigDecimal.ONE.subtract(rate))
                    .setScale(2, RoundingMode.HALF_UP);
            finalAmount = orderTotal.subtract(discountAmount);
        }

        // 计算积分抵现（互斥时第98-103行已处理冲突，此处只需判断积分>0即可）
        if (redeemPoints > 0) {
            int redeemRate = getPointsRedeemRate();
            if (redeemRate <= 0) redeemRate = 100;

            // 计算最大可抵扣金额
            BigDecimal maxRedeemAmount = finalAmount
                    .multiply(getRedeemMaxPercent())
                    .setScale(2, RoundingMode.HALF_UP);
            int maxRedeemPoints = maxRedeemAmount
                    .multiply(BigDecimal.valueOf(redeemRate))
                    .intValue();

            // 限制用户可用积分
            actualRedeemPoints = Math.min(redeemPoints, member.getAvailablePoints());
            actualRedeemPoints = Math.min(actualRedeemPoints, maxRedeemPoints);

            // 抵扣金额 = 抵扣积分 / 抵扣比例，向下取整到分
            redeemAmount = BigDecimal.valueOf(actualRedeemPoints)
                    .divide(BigDecimal.valueOf(redeemRate), 2, RoundingMode.DOWN);

            finalAmount = finalAmount.subtract(redeemAmount);
            if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
                finalAmount = BigDecimal.ZERO;
            }
        }

        vo.setDiscountAmount(discountAmount);
        // 最大可抵扣积分 = min(可用积分, 基于订单金额30%上限)
        int maxByPercent = finalAmount.multiply(new BigDecimal(redeemRate))
                .multiply(getRedeemMaxPercent())
                .setScale(0, RoundingMode.DOWN).intValue();
        vo.setMaxRedeemPoints(Math.min(member.getAvailablePoints(), maxByPercent));
        vo.setRedeemPoints(actualRedeemPoints);
        vo.setRedeemAmount(redeemAmount);
        vo.setFinalPayAmount(finalAmount);

        // 预计获得积分（基于实付金额）
        int pointsPerYuan = getPointsPerYuan();
        int expectedPoints = finalAmount.multiply(BigDecimal.valueOf(pointsPerYuan))
                .setScale(0, RoundingMode.DOWN).intValue();
        vo.setExpectedPoints(expectedPoints);

        log.info("会员结算: memberId={}, 原价={}, 折扣={}, 抵现={}, 实付={}",
                memberId, orderTotal, discountAmount, redeemAmount, finalAmount);

        return vo;
    }
}
