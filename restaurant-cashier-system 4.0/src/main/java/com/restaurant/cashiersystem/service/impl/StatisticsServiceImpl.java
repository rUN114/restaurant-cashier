package com.restaurant.cashiersystem.service.impl;

import com.restaurant.cashiersystem.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ========= 原有菜品销售统计 =========
    @Override
    public List<Map<String, Object>> getDishSalesCount(Long storeId, String month) {
        String sql = "SELECT oi.dish_name AS name, SUM(oi.quantity) AS count " +
                "FROM order_items oi " +
                "JOIN orders o ON oi.order_id = o.id " +
                "WHERE o.status = 'settled' " +
                "AND DATE_FORMAT(o.settled_at, '%Y-%m') = ? ";
        List<Object> params = new ArrayList<>();
        params.add(month);
        if (storeId != null) {
            sql += "AND o.store_id = ? ";
            params.add(storeId);
        }
        sql += "GROUP BY oi.dish_name ORDER BY count DESC LIMIT 20";
        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    @Override
    public List<Map<String, Object>> getDishSalesAmount(Long storeId, String month) {
        String sql = "SELECT oi.dish_name AS name, SUM(oi.price * oi.quantity) AS amount " +
                "FROM order_items oi " +
                "JOIN orders o ON oi.order_id = o.id " +
                "WHERE o.status = 'settled' " +
                "AND DATE_FORMAT(o.settled_at, '%Y-%m') = ? ";
        List<Object> params = new ArrayList<>();
        params.add(month);
        if (storeId != null) {
            sql += "AND o.store_id = ? ";
            params.add(storeId);
        }
        sql += "GROUP BY oi.dish_name ORDER BY amount DESC LIMIT 20";
        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    @Override
    public List<Map<String, Object>> getCategorySales(Long storeId, String month) {
        String sql = "SELECT c.name AS name, SUM(oi.quantity) AS count, SUM(oi.price * oi.quantity) AS amount " +
                "FROM order_items oi " +
                "JOIN orders o ON oi.order_id = o.id " +
                "JOIN dishes d ON oi.dish_id = d.id " +
                "JOIN categories c ON d.category_id = c.id " +
                "WHERE o.status = 'settled' " +
                "AND DATE_FORMAT(o.settled_at, '%Y-%m') = ? ";
        List<Object> params = new ArrayList<>();
        params.add(month);
        if (storeId != null) {
            sql += "AND o.store_id = ? ";
            params.add(storeId);
        }
        sql += "GROUP BY c.name ORDER BY amount DESC";
        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    @Override
    public List<Map<String, Object>> getStoreSales(String month) {
        String sql = "SELECT s.name AS storeName, SUM(o.final_amount) AS amount " +
                "FROM orders o " +
                "JOIN stores s ON o.store_id = s.id " +
                "WHERE o.status = 'settled' " +
                "AND DATE_FORMAT(o.settled_at, '%Y-%m') = ? " +
                "GROUP BY s.name ORDER BY amount DESC";
        return jdbcTemplate.queryForList(sql, month);
    }

    // ========= 新增营业额统计 =========
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
        String sql = "SELECT COALESCE(SUM(final_amount),0) FROM orders WHERE status='settled' AND DATE_FORMAT(settled_at,'%Y-%m')=DATE_FORMAT(NOW(),'%Y-%m')";
        List<Object> params = new ArrayList<>();
        if (storeId != null) {
            sql += " AND store_id=?";
            params.add(storeId);
        }
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, params.toArray());
    }

    @Override
    public List<Map<String, Object>> getDailyTurnoverTrend(String month, Long storeId) {
        String sql = "SELECT DATE(settled_at) AS day, SUM(final_amount) AS amount FROM orders WHERE status='settled' AND DATE_FORMAT(settled_at,'%Y-%m')=?";
        List<Object> params = new ArrayList<>();
        params.add(month);
        if (storeId != null) {
            sql += " AND store_id=?";
            params.add(storeId);
        }
        sql += " GROUP BY DATE(settled_at) ORDER BY day";
        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    @Override
    public List<Map<String, Object>> getPaymentTypeStatistics(String month, Long storeId) {
        String sql = "SELECT payment_method AS name, SUM(final_amount) AS amount FROM orders WHERE status='settled' AND DATE_FORMAT(settled_at,'%Y-%m')=?";
        List<Object> params = new ArrayList<>();
        params.add(month);
        if (storeId != null) {
            sql += " AND store_id=?";
            params.add(storeId);
        }
        sql += " GROUP BY payment_method";
        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    @Override
    public List<Map<String, Object>> getStoreMonthTurnover(String month) {
        String sql = "SELECT s.name AS storeName, SUM(o.final_amount) AS amount FROM orders o JOIN stores s ON o.store_id=s.id WHERE o.status='settled' AND DATE_FORMAT(o.settled_at,'%Y-%m')=? GROUP BY s.name ORDER BY amount DESC";
        return jdbcTemplate.queryForList(sql, month);
    }

    // ========= 服务员专用：自己开单的今日营业额 =========
    @Override
    public BigDecimal getTodayTurnoverForWaiter(Long userId) {
        String sql = "SELECT COALESCE(SUM(final_amount),0) FROM orders WHERE status='settled' AND created_by=? AND DATE(settled_at)=CURDATE()";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, userId);
    }
}