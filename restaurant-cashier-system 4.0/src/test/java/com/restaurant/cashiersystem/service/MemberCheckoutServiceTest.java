package com.restaurant.cashiersystem.service;

import com.restaurant.cashiersystem.dto.MemberCheckoutVO;
import com.restaurant.cashiersystem.entity.Member;
import com.restaurant.cashiersystem.entity.MemberLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("会员结算引擎 — 折扣/积分/互斥规则")
class MemberCheckoutServiceTest {

    @Mock private MemberService memberService;
    @Mock private MemberLevelService memberLevelService;
    @Mock private SysConfigService sysConfigService;

    @InjectMocks
    private MemberCheckoutService checkoutService;

    private Member member;
    private MemberLevel goldLevel;
    private MemberLevel silverLevel;

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setId(1L);
        member.setName("张三");
        member.setPhone("13800001111");
        member.setStatus(1);
        member.setAvailablePoints(1000);
        member.setLevelId(2L);

        goldLevel = new MemberLevel();
        goldLevel.setId(2L);
        goldLevel.setName("金卡会员");
        goldLevel.setDiscountRate(new BigDecimal("8.0"));     // 8折
        goldLevel.setSortOrder(2);
        goldLevel.setStatus(1);

        silverLevel = new MemberLevel();
        silverLevel.setId(1L);
        silverLevel.setName("银卡会员");
        silverLevel.setDiscountRate(new BigDecimal("9.5"));   // 9.5折
        silverLevel.setSortOrder(1);
        silverLevel.setStatus(1);

        // 默认配置（lenient 避免非必要 stub 报错）
        when(sysConfigService.getIntValue(eq("member.points_per_yuan"), eq(1))).thenReturn(1);
        when(sysConfigService.getIntValue(eq("member.points_redeem_rate"), eq(100))).thenReturn(100);
        when(sysConfigService.getBoolValue(eq("member.discount_points_mutex"), eq(true))).thenReturn(true);
        when(sysConfigService.getIntValue(eq("member.points_redeem_max_percent"), eq(30)))
                .thenReturn(30);
    }

    @Test
    @DisplayName("会员不存在 → 返回 null")
    void shouldReturnNullWhenMemberNotFound() {
        when(memberService.getById(1L)).thenReturn(null);
        assertNull(checkoutService.calculate(1L, BigDecimal.valueOf(100), false, 0));
    }

    @Test
    @DisplayName("会员被禁用 → 返回 null")
    void shouldReturnNullWhenMemberDisabled() {
        member.setStatus(0);
        when(memberService.getById(1L)).thenReturn(member);
        assertNull(checkoutService.calculate(1L, BigDecimal.valueOf(100), false, 0));
    }

    @Test
    @DisplayName("订单金额为 0 → 返回 null")
    void shouldReturnNullWhenAmountZero() {
        assertNull(checkoutService.calculate(1L, BigDecimal.ZERO, false, 0));
    }

    @Test
    @DisplayName("会员 ID 为 null → 返回 null")
    void shouldReturnNullWhenMemberIdNull() {
        assertNull(checkoutService.calculate(null, BigDecimal.valueOf(100), false, 0));
    }

    @Test
    @DisplayName("金卡 8 折：原价 200 → 折扣 40 → 实付 160")
    void goldCardDiscount() {
        when(memberService.getById(1L)).thenReturn(member);
        when(memberLevelService.getById(2L)).thenReturn(goldLevel);

        MemberCheckoutVO vo = checkoutService.calculate(1L,
                BigDecimal.valueOf(200), true, 0);

        assertNotNull(vo);
        assertEquals("金卡会员", vo.getLevelName());
        assertEquals(0, BigDecimal.valueOf(40).compareTo(vo.getDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(160).compareTo(vo.getFinalPayAmount()));
        assertEquals(0, vo.getRedeemPoints());
    }

    @Test
    @DisplayName("积分抵现：1000 分按 100:1 抵扣 10 元")
    void pointsRedeem() {
        when(memberService.getById(1L)).thenReturn(member);
        when(memberLevelService.getById(2L)).thenReturn(goldLevel);

        MemberCheckoutVO vo = checkoutService.calculate(1L,
                BigDecimal.valueOf(100), false, 500);

        assertNotNull(vo);
        assertEquals(0, BigDecimal.valueOf(5).compareTo(vo.getRedeemAmount()));
        assertEquals(0, BigDecimal.valueOf(95).compareTo(vo.getFinalPayAmount()));
        assertEquals(500, vo.getRedeemPoints());
    }

    @Test
    @DisplayName("折扣与积分互斥：同时使用 → 积分失效，只用折扣")
    void discountAndPointsMutex() {
        when(memberService.getById(1L)).thenReturn(member);
        when(memberLevelService.getById(2L)).thenReturn(goldLevel);

        // 互斥 + 折扣优先
        MemberCheckoutVO vo = checkoutService.calculate(1L,
                BigDecimal.valueOf(200), true, 500);

        assertNotNull(vo);
        assertEquals(0, vo.getRedeemPoints());  // 积分抵扣被忽略
        assertTrue(vo.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0); // 折扣生效
    }

    @Test
    @DisplayName("会员积分不足 → 只抵扣可用部分")
    void pointsNotEnough() {
        member.setAvailablePoints(200);  // 只有 200 分
        when(memberService.getById(1L)).thenReturn(member);
        when(memberLevelService.getById(2L)).thenReturn(goldLevel);

        MemberCheckoutVO vo = checkoutService.calculate(1L,
                BigDecimal.valueOf(100), false, 500); // 想抵 500 分

        assertNotNull(vo);
        assertEquals(200, vo.getRedeemPoints());  // 实际只抵了 200
        assertTrue(vo.getRedeemAmount().compareTo(BigDecimal.valueOf(2)) == 0);
    }

    @Test
    @DisplayName("积分抵扣最大值受限于订单金额的 30%")
    void pointsRedeemCapAt30Percent() {
        member.setAvailablePoints(10000);
        when(memberService.getById(1L)).thenReturn(member);
        when(memberLevelService.getById(2L)).thenReturn(goldLevel);

        // 订单 100 元，30% = 30 元，按 100:1 = 最多 3000 分
        MemberCheckoutVO vo = checkoutService.calculate(1L,
                BigDecimal.valueOf(100), false, 10000);

        assertNotNull(vo);
        assertTrue(vo.getRedeemPoints() <= 3000);
        assertTrue(vo.getRedeemAmount().compareTo(BigDecimal.valueOf(30)) <= 0);
    }

    @Test
    @DisplayName("预计获得积分：实付 160 × 1分/元 = 160 分")
    void expectedPoints() {
        when(memberService.getById(1L)).thenReturn(member);
        when(memberLevelService.getById(2L)).thenReturn(goldLevel);

        MemberCheckoutVO vo = checkoutService.calculate(1L,
                BigDecimal.valueOf(200), true, 0);

        assertNotNull(vo);
        assertEquals(160, vo.getExpectedPoints()); // 实付 160 × 1
    }

    @Test
    @DisplayName("银卡 9.5 折：等级不同折扣不同")
    void silverCardDiscount() {
        member.setLevelId(1L);
        when(memberService.getById(1L)).thenReturn(member);
        when(memberLevelService.getById(1L)).thenReturn(silverLevel);

        MemberCheckoutVO vo = checkoutService.calculate(1L,
                BigDecimal.valueOf(200), true, 0);

        assertNotNull(vo);
        assertEquals(0, BigDecimal.valueOf(10).compareTo(vo.getDiscountAmount()));  // 200 × 5%
        assertEquals(0, BigDecimal.valueOf(190).compareTo(vo.getFinalPayAmount()));
    }
}
