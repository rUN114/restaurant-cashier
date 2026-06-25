package com.restaurant.cashiersystem.controller;

import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.dto.MemberCheckoutVO;
import com.restaurant.cashiersystem.service.MemberCheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "会员结算", description = "收银台会员折扣/积分抵现计算")
@RestController
@RequestMapping("/api/member-checkout")
public class MemberCheckoutController {

    @Autowired private MemberCheckoutService checkoutService;

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','cashier','waiter')")
    @Operation(summary = "计算会员结算信息（折扣、抵现、实付）")
    public Result<MemberCheckoutVO> calculate(
            @RequestParam Long memberId,
            @RequestParam BigDecimal orderTotal,
            @RequestParam(defaultValue = "false") boolean useDiscount,
            @RequestParam(defaultValue = "0") int redeemPoints) {
        MemberCheckoutVO vo = checkoutService.calculate(memberId, orderTotal, useDiscount, redeemPoints);
        if (vo == null) return Result.error("会员不可用或订单金额无效");
        return Result.success(vo);
    }
}
