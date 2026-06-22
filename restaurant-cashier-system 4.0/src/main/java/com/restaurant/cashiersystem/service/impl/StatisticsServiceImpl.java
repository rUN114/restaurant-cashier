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
        YearMonth ym = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.plusMonths(1).atDay(1);
        return new Date[] {
            java.sql.Timestamp.valueOf(start.atStartOfDay()),
            java.sql.Timestamp.valueOf(end.atStartOfDay())
        };
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
}