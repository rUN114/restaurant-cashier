package com.restaurant.cashiersystem.controller;

import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.dto.DashboardVO;
import com.restaurant.cashiersystem.service.DashboardService;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "老板看板", description = "首页实时经营指标")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/today")
    @Operation(summary = "今日经营概览（营业额/利润/客单/环比）")
    public Result<DashboardVO> today(HttpServletRequest request) {
        String roleCode = getRoleFromToken(request);
        Long storeId = null;

        if ("admin_store".equals(roleCode)) {
            storeId = getStoreIdFromToken(request);
        }
        // admin_global → storeId=null 看全部
        // waiter/cashier → storeId 从 token 取
        if (storeId == null && !"admin_global".equals(roleCode)) {
            storeId = getStoreIdFromToken(request);
        }

        return Result.success(dashboardService.getTodayDashboard(storeId));
    }

    private String getRoleFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parseToken(authHeader.substring(7));
                return (String) claims.get("roleCode");
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Long getStoreIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parseToken(authHeader.substring(7));
                return claims.get("storeId", Long.class);
            } catch (Exception ignored) {}
        }
        return null;
    }
}
