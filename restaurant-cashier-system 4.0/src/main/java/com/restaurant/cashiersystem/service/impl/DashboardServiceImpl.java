package com.restaurant.cashiersystem.service.impl;

import com.restaurant.cashiersystem.dto.DashboardVO;
import com.restaurant.cashiersystem.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public DashboardVO getTodayDashboard(Long storeId) {
        DashboardVO vo = new DashboardVO();

        String storeFilter = storeId != null ? " AND o.store_id = " + storeId : "";

        // 今日营业额 + 订单数
        String todaySql = "SELECT COALESCE(SUM(o.final_amount), 0) AS revenue, " +
                "COUNT(*) AS order_count " +
                "FROM orders o WHERE o.status = 'settled' " +
                "AND DATE(o.settled_at) = CURDATE()" + storeFilter;
        Map<String, Object> todayRow = jdbcTemplate.queryForMap(todaySql);
        vo.setTodayRevenue((BigDecimal) todayRow.get("revenue"));
        vo.setTodayOrderCount(((Number) todayRow.get("order_count")).intValue());

        // 昨日营业额（环比）
        String ydaySql = "SELECT COALESCE(SUM(o.final_amount), 0) AS revenue " +
                "FROM orders o WHERE o.status = 'settled' " +
                "AND DATE(o.settled_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)" + storeFilter;
        Map<String, Object> ydayRow = jdbcTemplate.queryForMap(ydaySql);
        vo.setYesterdayRevenue((BigDecimal) ydayRow.get("revenue"));

        // 客单价 = 营业额 / 订单数
        int count = vo.getTodayOrderCount();
        vo.setAvgOrderValue(count > 0
                ? vo.getTodayRevenue().divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // 今日毛利（营业额 - 原料成本）
        String profitSql = "SELECT " +
                "COALESCE(SUM(oi.price * oi.quantity), 0) AS sales, " +
                "COALESCE(SUM(c.cost_unit * oi.quantity), 0) AS cost " +
                "FROM order_items oi " +
                "JOIN orders o ON oi.order_id = o.id AND o.status = 'settled' AND DATE(o.settled_at) = CURDATE()" +
                (storeId != null ? " AND o.store_id = " + storeId : "") +
                " LEFT JOIN (" +
                "  SELECT dr.dish_id, dr.store_id, " +
                "  SUM(dr.used_quantity * COALESCE(mi.cost_price, 0)) AS cost_unit " +
                "  FROM dish_recipes dr " +
                "  LEFT JOIN material_inventory mi ON dr.material_id = mi.material_id " +
                "  AND dr.store_id = mi.store_id GROUP BY dr.dish_id, dr.store_id" +
                ") c ON oi.dish_id = c.dish_id AND o.store_id = c.store_id";
        Map<String, Object> profitRow = jdbcTemplate.queryForMap(profitSql);
        BigDecimal sales = (BigDecimal) profitRow.get("sales");
        BigDecimal cost = (BigDecimal) profitRow.get("cost");
        vo.setTodayProfit(sales.subtract(cost));
        vo.setProfitMargin(sales.compareTo(BigDecimal.ZERO) > 0
                ? vo.getTodayProfit().divide(sales, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        return vo;
    }
}
