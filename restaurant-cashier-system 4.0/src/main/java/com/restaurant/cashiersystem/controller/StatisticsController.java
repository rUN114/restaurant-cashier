package com.restaurant.cashiersystem.controller;

import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.service.StatisticsService;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "统计分析", description = "菜品销量、营业额、支付方式、趋势等综合统计")
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private JwtUtil jwtUtil;

    // ========= 菜品销售统计 =========
    @GetMapping("/dish-sales-count")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<List<Map<String, Object>>> dishSalesCount(
            @RequestParam(required = false) Long storeId,
            @RequestParam String month) {
        return Result.success(statisticsService.getDishSalesCount(storeId, month));
    }

    @GetMapping("/dish-sales-amount")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<List<Map<String, Object>>> dishSalesAmount(
            @RequestParam(required = false) Long storeId,
            @RequestParam String month) {
        return Result.success(statisticsService.getDishSalesAmount(storeId, month));
    }

    @GetMapping("/category-sales")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<List<Map<String, Object>>> categorySales(
            @RequestParam(required = false) Long storeId,
            @RequestParam String month) {
        return Result.success(statisticsService.getCategorySales(storeId, month));
    }

    @GetMapping("/store-sales")
    @PreAuthorize("hasAuthority('admin_global')")
    public Result<List<Map<String, Object>>> storeSales(@RequestParam String month) {
        return Result.success(statisticsService.getStoreSales(month));
    }

    // ========= 营业额统计 =========
    @GetMapping("/store-turnover")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<BigDecimal> storeTurnover(@RequestParam(required = false) Long storeId) {
        return Result.success(statisticsService.getStoreTurnover(storeId));
    }

    /**
     * 今日营业额（融合后：支持服务员看自己、其他人看门店）
     */
    @GetMapping("/today-turnover")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','cashier','waiter')")
    public Result<BigDecimal> todayTurnover(
            @RequestParam(required = false) Long storeId,
            HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Claims claims = jwtUtil.parseToken(token);
            String roleCode = (String) claims.get("roleCode");
            Long userId = Long.valueOf(claims.getSubject());

            // 服务员：只看自己开单的今日营业额
            if ("waiter".equals(roleCode)) {
                BigDecimal amount = statisticsService.getTodayTurnoverForWaiter(userId);
                return Result.success(amount);
            }
            // 管理员、店长、收银员：按门店查询
            return Result.success(statisticsService.getTodayTurnover(storeId));
        }
        return Result.error("未登录");
    }

    @GetMapping("/month-turnover")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<BigDecimal> monthTurnover(@RequestParam(required = false) Long storeId) {
        return Result.success(statisticsService.getMonthTurnover(storeId));
    }

    @GetMapping("/daily-turnover-trend")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<List<Map<String, Object>>> dailyTurnoverTrend(
            @RequestParam String month,
            @RequestParam(required = false) Long storeId) {
        return Result.success(statisticsService.getDailyTurnoverTrend(month, storeId));
    }

    @GetMapping("/payment-type-statistics")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<List<Map<String, Object>>> paymentTypeStatistics(
            @RequestParam String month,
            @RequestParam(required = false) Long storeId) {
        return Result.success(statisticsService.getPaymentTypeStatistics(month, storeId));
    }

    @GetMapping("/store-month-turnover")
    @PreAuthorize("hasAuthority('admin_global')")
    public Result<List<Map<String, Object>>> storeMonthTurnover(@RequestParam String month) {
        return Result.success(statisticsService.getStoreMonthTurnover(month));
    }

    /** 菜品成本卡：售价 - 配方原料成本 = 理论毛利 */
    @GetMapping("/dish-cost-card")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<List<Map<String, Object>>> dishCostCard(@RequestParam(required = false) Long storeId) {
        List<Object> params = new java.util.ArrayList<>();
        String sql = "SELECT d.id, d.name, d.price, " +
            "COALESCE(SUM(dr.used_quantity * COALESCE(mi.cost_price, 0)), 0) AS cost, " +
            "CASE WHEN d.price > 0 THEN ROUND((d.price - COALESCE(SUM(dr.used_quantity * COALESCE(mi.cost_price, 0)), 0)) / d.price * 100, 1) ELSE 0 END AS margin " +
            "FROM dishes d LEFT JOIN dish_recipes dr ON dr.dish_id = d.id " +
            "LEFT JOIN material_inventory mi ON mi.material_id = dr.material_id AND mi.store_id = d.store_id " +
            "WHERE d.deleted_at IS NULL";
        if (storeId != null) {
            sql += " AND d.store_id = ?";
            params.add(storeId);
        }
        sql += " GROUP BY d.id, d.name, d.price ORDER BY margin DESC";
        return Result.success(jdbcTemplate.queryForList(sql, params.toArray()));
    }

    /** 高峰时段分析：按小时统计订单数 */
    @GetMapping("/peak-hours")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<List<Map<String, Object>>> peakHours(@RequestParam(required = false) String month,
                                                        @RequestParam(required = false) Long storeId) {
        List<Object> params = new java.util.ArrayList<>();
        String sql = "SELECT HOUR(created_at) AS hour, COUNT(*) AS cnt " +
                "FROM orders WHERE status = 'settled'";
        if (storeId != null) {
            sql += " AND store_id = ?";
            params.add(storeId);
        }
        if (month != null) {
            sql += " AND DATE_FORMAT(created_at, '%Y-%m') = ?";
            params.add(month);
        }
        sql += " GROUP BY HOUR(created_at) ORDER BY hour";
        return Result.success(jdbcTemplate.queryForList(sql, params.toArray()));
    }

    /** 翻台率：每张桌今天被用了几次 */
    @GetMapping("/table-turnover")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<List<Map<String, Object>>> tableTurnover(@RequestParam(required = false) Long storeId) {
        List<Object> params = new java.util.ArrayList<>();
        String sql = "SELECT o.table_number, COUNT(*) AS times, " +
                "COALESCE(SUM(o.final_amount), 0) AS total " +
                "FROM orders o WHERE o.status = 'settled' AND DATE(o.settled_at) = CURDATE()";
        if (storeId != null) {
            sql += " AND o.store_id = ?";
            params.add(storeId);
        }
        sql += " GROUP BY o.table_number ORDER BY times DESC";
        return Result.success(jdbcTemplate.queryForList(sql, params.toArray()));
    }

    @GetMapping("/gross-profit")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<List<Map<String, Object>>> grossProfit(
            @RequestParam String month,
            @RequestParam(required = false) Long storeId,
            HttpServletRequest request) {
        // 店长只能查看本门店毛利
        String roleCode = getRoleFromToken(request);
        if ("admin_store".equals(roleCode)) {
            storeId = getStoreIdFromToken(request);
        }
        return Result.success(statisticsService.getGrossProfit(storeId, month));
    }

    /** 今日收银员绩效排行 */
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @GetMapping("/cashier-ranking")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<List<Map<String, Object>>> cashierRanking(@RequestParam(required = false) Long storeId) {
        List<Object> params = new java.util.ArrayList<>();
        String sql = "SELECT o.settled_by, COALESCE(u.real_name, u.username, '未知') AS name, " +
                "COUNT(*) AS orders, COALESCE(SUM(o.final_amount),0) AS amount, " +
                "COALESCE((SELECT COUNT(*) FROM order_items oi WHERE oi.return_reason IS NOT NULL " +
                " AND oi.store_id = o.store_id AND DATE(oi.created_at) = CURDATE()), 0) AS returns " +
                "FROM orders o LEFT JOIN users u ON o.settled_by = u.id " +
                "WHERE o.status = 'settled' AND DATE(o.settled_at) = CURDATE()";
        if (storeId != null) {
            sql += " AND o.store_id = ?";
            params.add(storeId);
        }
        sql += " GROUP BY o.settled_by, u.real_name, u.username, o.store_id ORDER BY amount DESC";
        return Result.success(jdbcTemplate.queryForList(sql, params.toArray()));
    }

    // ===== token 解析工具方法 =====
    private String getRoleFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parseToken(authHeader.substring(7));
                return (String) claims.get("roleCode");
            } catch (Exception e) { /* ignore */ }
        }
        return null;
    }

    private Long getStoreIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parseToken(authHeader.substring(7));
                return claims.get("storeId", Long.class);
            } catch (Exception e) { /* ignore */ }
        }
        return null;
    }
}