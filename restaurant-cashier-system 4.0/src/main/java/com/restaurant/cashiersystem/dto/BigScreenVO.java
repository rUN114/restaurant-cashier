package com.restaurant.cashiersystem.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/** 实时营业大屏数据 */
@Data
public class BigScreenVO {
    private BigDecimal todayRevenue;
    private int todayOrderCount;
    private int occupiedTables;
    private int totalTables;
    private BigDecimal yesterdayRevenue;
    private BigDecimal revenueChange; // 百分比
    private List<RecentOrder> recentOrders;
    private List<DiningOrder> diningOrders;

    @Data
    public static class RecentOrder {
        private String tableNumber;
        private BigDecimal amount;
        private String time;
    }

    @Data
    public static class DiningOrder {
        private String tableNumber;
        private int dishCount;
        private int elapsedMinutes;
    }
}
