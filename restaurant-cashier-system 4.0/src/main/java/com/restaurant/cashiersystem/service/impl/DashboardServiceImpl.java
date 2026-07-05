package com.restaurant.cashiersystem.service.impl;

import com.restaurant.cashiersystem.dto.BigScreenVO;
import com.restaurant.cashiersystem.dto.DashboardVO;
import com.restaurant.cashiersystem.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** SQL 聚合查询均返回单行，使用 queryForMap 替代 queryForMap 并在无结果时返回默认 Map */
    private Map<String, Object> safeQueryForMap(String sql, Object... params) {
        try {
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, params);
            return list.isEmpty() ? Collections.emptyMap() : list.get(0);
        } catch (EmptyResultDataAccessException e) {
            return Collections.emptyMap();
        }
    }

    private BigDecimal getBigDecimal(Map<String, Object> row, String key) {
        Object val = row.get(key);
        return val instanceof BigDecimal ? (BigDecimal) val : BigDecimal.ZERO;
    }

    private int getInt(Map<String, Object> row, String key) {
        Object val = row.get(key);
        return val instanceof Number ? ((Number) val).intValue() : 0;
    }

    @Override
    public BigScreenVO getBigScreen(Long storeId) {
        BigScreenVO vo = new BigScreenVO();
        List<Object> sfParams = storeId != null ? List.of(storeId) : List.of();
        String sf = storeId != null ? " AND o.store_id = ?" : "";
        String tsf = storeId != null ? " WHERE store_id = ?" : "";

        // 今日营业额 + 订单数
        String todaySql = "SELECT COALESCE(SUM(o.final_amount),0) AS revenue, COUNT(*) AS cnt " +
                "FROM orders o WHERE o.status = 'settled' AND DATE(o.settled_at) = CURDATE()" + sf;
        Map<String, Object> todayRow = safeQueryForMap(todaySql, sfParams.toArray());
        vo.setTodayRevenue(getBigDecimal(todayRow, "revenue"));
        vo.setTodayOrderCount(getInt(todayRow, "cnt"));

        // 昨日营业额
        String ydaySql = "SELECT COALESCE(SUM(o.final_amount),0) AS revenue " +
                "FROM orders o WHERE o.status = 'settled' AND DATE(o.settled_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)" + sf;
        Map<String, Object> ydayRow = safeQueryForMap(ydaySql, sfParams.toArray());
        BigDecimal yesterday = getBigDecimal(ydayRow, "revenue");
        vo.setYesterdayRevenue(yesterday);

        // 环比
        if (yesterday.compareTo(BigDecimal.ZERO) > 0) {
            vo.setRevenueChange(vo.getTodayRevenue().subtract(yesterday)
                    .divide(yesterday, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));
        } else {
            vo.setRevenueChange(BigDecimal.ZERO);
        }

        // 桌台占用
        String tableSql = "SELECT COUNT(*) AS total, " +
                "COALESCE(SUM(CASE WHEN status = 'occupied' THEN 1 ELSE 0 END), 0) AS occupied " +
                "FROM tables" + tsf;
        Map<String, Object> tableRow = safeQueryForMap(tableSql, sfParams.toArray());
        vo.setTotalTables(getInt(tableRow, "total"));
        vo.setOccupiedTables(getInt(tableRow, "occupied"));

        // 最近5笔成交
        String recentSql = "SELECT o.table_number, o.final_amount, o.settled_at " +
                "FROM orders o WHERE o.status = 'settled' AND DATE(o.settled_at) = CURDATE()" + sf +
                " ORDER BY o.settled_at DESC LIMIT 5";
        List<BigScreenVO.RecentOrder> recentOrders = new ArrayList<>();
        jdbcTemplate.query(recentSql, sfParams.toArray(), rs -> {
            BigScreenVO.RecentOrder ro = new BigScreenVO.RecentOrder();
            ro.setTableNumber(rs.getString("table_number"));
            ro.setAmount(rs.getBigDecimal("final_amount"));
            var t = rs.getTimestamp("settled_at");
            ro.setTime(t != null ? String.format("%02d:%02d", t.toLocalDateTime().getHour(), t.toLocalDateTime().getMinute()) : "");
            recentOrders.add(ro);
        });
        vo.setRecentOrders(recentOrders);

        // 正在用餐的订单（等餐中）
        String diningSql = "SELECT o.table_number, COUNT(oi.id) AS dish_cnt, " +
                "TIMESTAMPDIFF(MINUTE, o.created_at, NOW()) AS elapsed " +
                "FROM orders o JOIN order_items oi ON oi.order_id = o.id " +
                "WHERE o.status = 'in_progress'" + sf +
                " AND oi.kitchen_status != 'SERVED' " +
                "GROUP BY o.id, o.table_number, o.created_at " +
                "ORDER BY o.created_at ASC LIMIT 8";
        List<BigScreenVO.DiningOrder> diningOrders = new ArrayList<>();
        jdbcTemplate.query(diningSql, sfParams.toArray(), rs -> {
            BigScreenVO.DiningOrder d = new BigScreenVO.DiningOrder();
            d.setTableNumber(rs.getString("table_number"));
            d.setDishCount(((Number) rs.getObject("dish_cnt")).intValue());
            d.setElapsedMinutes(((Number) rs.getObject("elapsed")).intValue());
            diningOrders.add(d);
        });
        vo.setDiningOrders(diningOrders);

        return vo;
    }

    @Override
    public DashboardVO getTodayDashboard(Long storeId) {
        DashboardVO vo = new DashboardVO();
        List<Object> sfParams = storeId != null ? List.of(storeId) : List.of();
        String sf = storeId != null ? " AND o.store_id = ?" : "";

        // 今日营业额 + 订单数
        String todaySql = "SELECT COALESCE(SUM(o.final_amount), 0) AS revenue, " +
                "COUNT(*) AS order_count " +
                "FROM orders o WHERE o.status = 'settled' " +
                "AND DATE(o.settled_at) = CURDATE()" + sf;
        Map<String, Object> todayRow = safeQueryForMap(todaySql, sfParams.toArray());
        vo.setTodayRevenue(getBigDecimal(todayRow, "revenue"));
        vo.setTodayOrderCount(getInt(todayRow, "order_count"));

        // 昨日营业额（环比）
        String ydaySql = "SELECT COALESCE(SUM(o.final_amount), 0) AS revenue " +
                "FROM orders o WHERE o.status = 'settled' " +
                "AND DATE(o.settled_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)" + sf;
        Map<String, Object> ydayRow = safeQueryForMap(ydaySql, sfParams.toArray());
        vo.setYesterdayRevenue(getBigDecimal(ydayRow, "revenue"));

        // 客单价 = 营业额 / 订单数
        int count = vo.getTodayOrderCount();
        vo.setAvgOrderValue(count > 0
                ? vo.getTodayRevenue().divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // 今日毛利
        String profitSql = "SELECT " +
                "COALESCE(SUM(oi.price * oi.quantity), 0) AS sales, " +
                "COALESCE(SUM(c.cost_unit * oi.quantity), 0) AS cost " +
                "FROM order_items oi " +
                "JOIN orders o ON oi.order_id = o.id AND o.status = 'settled' AND DATE(o.settled_at) = CURDATE()" +
                (storeId != null ? " AND o.store_id = ?" : "") +
                " LEFT JOIN (" +
                "  SELECT dr.dish_id, dr.store_id, " +
                "  SUM(dr.used_quantity * COALESCE(mi.cost_price, 0) / COALESCE(NULLIF(m.unit_rate, 0), 1)) AS cost_unit " +
                "  FROM dish_recipes dr " +
                "  LEFT JOIN material_inventory mi ON dr.material_id = mi.material_id " +
                "  AND dr.store_id = mi.store_id " +
                "  LEFT JOIN materials m ON dr.material_id = m.id " +
                "  GROUP BY dr.dish_id, dr.store_id" +
                ") c ON oi.dish_id = c.dish_id AND o.store_id = c.store_id";
        Map<String, Object> profitRow = safeQueryForMap(profitSql, sfParams.toArray());
        BigDecimal sales = getBigDecimal(profitRow, "sales");
        BigDecimal cost = getBigDecimal(profitRow, "cost");
        vo.setTodayProfit(sales.subtract(cost));
        vo.setProfitMargin(sales.compareTo(BigDecimal.ZERO) > 0
                ? vo.getTodayProfit().divide(sales, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        return vo;
    }
}
