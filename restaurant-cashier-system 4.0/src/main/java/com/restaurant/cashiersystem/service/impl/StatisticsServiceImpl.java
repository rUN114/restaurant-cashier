package com.restaurant.cashiersystem.service.impl;

import com.restaurant.cashiersystem.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 将 "2026-06" 格式的月份字符串转换为日期范围。
     * 返回 [startOfMonth, startOfNextMonth)，用于索引友好查询替代 DATE_FORMAT。
     */
    private static Date[] monthRange(String month) {
        if (month == null || month.isBlank()) {
            throw new IllegalArgumentException("月份参数不能为空");
        }
        try {
            YearMonth ym = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.plusMonths(1).atDay(1);
            return new Date[] {
                java.sql.Timestamp.valueOf(start.atStartOfDay()),
                java.sql.Timestamp.valueOf(end.atStartOfDay())
            };
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("月份格式错误，应为 yyyy-MM: " + month, e);
        }
    }

    // ========= 原有菜品销售统计 =========
    @Override
    public List<Map<String, Object>> getDishSalesCount(Long storeId, String month) {
        Date[] range = monthRange(month);
        String sql = "SELECT oi.dish_name AS name, SUM(oi.quantity) AS count " +
                "FROM order_items oi " +
                "JOIN orders o ON oi.order_id = o.id " +
                "WHERE o.status = 'settled' " +
                "AND o.settled_at >= ? AND o.settled_at < ? ";
        List<Object> params = new ArrayList<>();
        params.add(range[0]); params.add(range[1]);
        if (storeId != null) {
            sql += "AND o.store_id = ? ";
            params.add(storeId);
        }
        sql += "GROUP BY oi.dish_name ORDER BY count DESC LIMIT 20";
        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    @Override
    public List<Map<String, Object>> getDishSalesAmount(Long storeId, String month) {
        Date[] range = monthRange(month);
        String sql = "SELECT oi.dish_name AS name, SUM(oi.price * oi.quantity) AS amount " +
                "FROM order_items oi " +
                "JOIN orders o ON oi.order_id = o.id " +
                "WHERE o.status = 'settled' " +
                "AND o.settled_at >= ? AND o.settled_at < ? ";
        List<Object> params = new ArrayList<>();
        params.add(range[0]); params.add(range[1]);
        if (storeId != null) {
            sql += "AND o.store_id = ? ";
            params.add(storeId);
        }
        sql += "GROUP BY oi.dish_name ORDER BY amount DESC LIMIT 20";
        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    @Override
    public List<Map<String, Object>> getCategorySales(Long storeId, String month) {
        Date[] range = monthRange(month);
        String sql = "SELECT c.name AS name, SUM(oi.quantity) AS count, SUM(oi.price * oi.quantity) AS amount " +
                "FROM order_items oi " +
                "JOIN orders o ON oi.order_id = o.id " +
                "JOIN dishes d ON oi.dish_id = d.id " +
                "JOIN categories c ON d.category_id = c.id " +
                "WHERE o.status = 'settled' " +
                "AND o.settled_at >= ? AND o.settled_at < ? ";
        List<Object> params = new ArrayList<>();
        params.add(range[0]); params.add(range[1]);
        if (storeId != null) {
            sql += "AND o.store_id = ? ";
            params.add(storeId);
        }
        sql += "GROUP BY c.name ORDER BY amount DESC";
        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    @Override
    public List<Map<String, Object>> getStoreSales(String month) {
        Date[] range = monthRange(month);
        String sql = "SELECT s.name AS storeName, SUM(o.final_amount) AS amount " +
                "FROM orders o " +
                "JOIN stores s ON o.store_id = s.id " +
                "WHERE o.status = 'settled' " +
                "AND o.settled_at >= ? AND o.settled_at < ? " +
                "GROUP BY s.name ORDER BY amount DESC";
        return jdbcTemplate.queryForList(sql, range[0], range[1]);
    }

    // ========= 营业额统计 =========
    @Override
    public BigDecimal getStoreTurnover(Long storeId) {
        String sql = "SELECT COALESCE(SUM(final_amount),0) FROM orders WHERE status='settled'";
        List<Object> params = new ArrayList<>();
        if (storeId != null) {
            sql += " AND store_id=?";
            params.add(storeId);
        }
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, params.toArray());
    }

    @Override
    public BigDecimal getTodayTurnover(Long storeId) {
        String sql = "SELECT COALESCE(SUM(final_amount),0) FROM orders WHERE status='settled' AND DATE(settled_at)=CURDATE()";
        List<Object> params = new ArrayList<>();
        if (storeId != null) {
            sql += " AND store_id=?";
            params.add(storeId);
        }
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, params.toArray());
    }

    @Override
    public BigDecimal getMonthTurnover(Long storeId) {
        // 范围查询替代 DATE_FORMAT，可利用 settled_at 索引
        LocalDate today = LocalDate.now();
        Date start = java.sql.Timestamp.valueOf(today.withDayOfMonth(1).atStartOfDay());
        Date end = java.sql.Timestamp.valueOf(today.plusMonths(1).withDayOfMonth(1).atStartOfDay());
        String sql = "SELECT COALESCE(SUM(final_amount),0) FROM orders WHERE status='settled' AND settled_at>=? AND settled_at<?";
        List<Object> params = new ArrayList<>();
        params.add(start); params.add(end);
        if (storeId != null) {
            sql += " AND store_id=?";
            params.add(storeId);
        }
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, params.toArray());
    }

    @Override
    public List<Map<String, Object>> getDailyTurnoverTrend(String month, Long storeId) {
        Date[] range = monthRange(month);
        String sql = "SELECT DATE(settled_at) AS day, SUM(final_amount) AS amount FROM orders WHERE status='settled' AND settled_at>=? AND settled_at<?";
        List<Object> params = new ArrayList<>();
        params.add(range[0]); params.add(range[1]);
        if (storeId != null) {
            sql += " AND store_id=?";
            params.add(storeId);
        }
        sql += " GROUP BY DATE(settled_at) ORDER BY day";
        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    @Override
    public List<Map<String, Object>> getPaymentTypeStatistics(String month, Long storeId) {
        Date[] range = monthRange(month);
        String sql = "SELECT payment_method AS name, SUM(final_amount) AS amount FROM orders WHERE status='settled' AND settled_at>=? AND settled_at<?";
        List<Object> params = new ArrayList<>();
        params.add(range[0]); params.add(range[1]);
        if (storeId != null) {
            sql += " AND store_id=?";
            params.add(storeId);
        }
        sql += " GROUP BY payment_method";
        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    @Override
    public List<Map<String, Object>> getStoreMonthTurnover(String month) {
        Date[] range = monthRange(month);
        String sql = "SELECT s.name AS storeName, SUM(o.final_amount) AS amount FROM orders o JOIN stores s ON o.store_id=s.id WHERE o.status='settled' AND o.settled_at>=? AND o.settled_at<? GROUP BY s.name ORDER BY amount DESC";
        return jdbcTemplate.queryForList(sql, range[0], range[1]);
    }

    // ========= 服务员专用：自己开单的今日营业额 =========
    @Override
    public BigDecimal getTodayTurnoverForWaiter(Long userId) {
        String sql = "SELECT COALESCE(SUM(final_amount),0) FROM orders WHERE status='settled' AND created_by=? AND DATE(settled_at)=CURDATE()";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, userId);
    }

    @Override
    public List<Map<String, Object>> getGrossProfit(Long storeId, String month) {
        Date[] range = monthRange(month);

        // 参数按 SQL 文本中 ? 从左到右顺序构建（子查询 → 外层 WHERE）
        List<Object> params = new ArrayList<>();

        // 构建成本子查询：将 storeId 推入子查询避免全量扫描
        StringBuilder costSub = new StringBuilder(
            "(SELECT dr.dish_id, dr.store_id, " +
            "SUM(dr.used_quantity * COALESCE(mi.cost_price, 0)) AS cost_per_unit " +
            "FROM dish_recipes dr " +
            "LEFT JOIN material_inventory mi ON dr.material_id = mi.material_id " +
            "AND dr.store_id = mi.store_id ");
        if (storeId != null) {
            costSub.append("WHERE dr.store_id = ? ");
            params.add(storeId);   // ← 子查询的 ? 排最前面（SQL 中先出现）
        }
        costSub.append("GROUP BY dr.dish_id, dr.store_id) c");

        // 外层 WHERE 的参数紧跟其后
        params.add(range[0]);
        params.add(range[1]);
        if (storeId != null) {
            params.add(storeId);   // ← 外层 WHERE 的 storeId（与子查询各需一个）
        }

        String sql = "SELECT oi.dish_name AS name, " +
                "SUM(oi.quantity) AS quantity, " +
                "ROUND(SUM(oi.price * oi.quantity), 2) AS sales, " +
                "ROUND(COALESCE(SUM(c.cost_per_unit * oi.quantity), 0), 2) AS cost, " +
                "ROUND(SUM(oi.price * oi.quantity) - COALESCE(SUM(c.cost_per_unit * oi.quantity), 0), 2) AS profit, " +
                "CASE WHEN SUM(oi.price * oi.quantity) > 0 " +
                "THEN ROUND((SUM(oi.price * oi.quantity) - COALESCE(SUM(c.cost_per_unit * oi.quantity), 0)) " +
                "/ SUM(oi.price * oi.quantity) * 100, 1) ELSE 0 END AS margin " +
                "FROM order_items oi " +
                "JOIN orders o ON oi.order_id = o.id " +
                "LEFT JOIN " + costSub.toString() + " " +
                "ON oi.dish_id = c.dish_id AND o.store_id = c.store_id " +
                "WHERE o.status = 'settled' AND o.settled_at >= ? AND o.settled_at < ? ";

        if (storeId != null) {
            sql += "AND o.store_id = ? ";
        }
        sql += "GROUP BY oi.dish_name ORDER BY profit DESC";
        return jdbcTemplate.queryForList(sql, params.toArray());
    }
}