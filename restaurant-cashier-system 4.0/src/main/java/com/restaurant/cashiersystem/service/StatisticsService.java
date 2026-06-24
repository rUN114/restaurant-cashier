package com.restaurant.cashiersystem.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface StatisticsService {

    // ========= 原有菜品销售统计（保留） =========
    List<Map<String, Object>> getDishSalesCount(Long storeId, String month);
    List<Map<String, Object>> getDishSalesAmount(Long storeId, String month);
    List<Map<String, Object>> getCategorySales(Long storeId, String month);
    List<Map<String, Object>> getStoreSales(String month);

    /** 毛利报表：每道菜的销售额、原料成本、毛利、毛利率 */
    List<Map<String, Object>> getGrossProfit(Long storeId, String month);

    // ========= 新增营业额统计（必须加上） =========
    BigDecimal getStoreTurnover(Long storeId);
    BigDecimal getTodayTurnover(Long storeId);
    BigDecimal getMonthTurnover(Long storeId);
    List<Map<String, Object>> getDailyTurnoverTrend(String month, Long storeId);
    List<Map<String, Object>> getPaymentTypeStatistics(String month, Long storeId);
    List<Map<String, Object>> getStoreMonthTurnover(String month);

    BigDecimal getTodayTurnoverForWaiter(Long userId);
}