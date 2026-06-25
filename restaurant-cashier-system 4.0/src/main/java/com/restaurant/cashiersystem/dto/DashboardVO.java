package com.restaurant.cashiersystem.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DashboardVO {
    /** 今日营业额 */
    private BigDecimal todayRevenue;
    /** 昨日营业额（用于环比） */
    private BigDecimal yesterdayRevenue;
    /** 今日订单数 */
    private int todayOrderCount;
    /** 今日客单价 */
    private BigDecimal avgOrderValue;
    /** 今日毛利（营业额 - 原料成本） */
    private BigDecimal todayProfit;
    /** 今日毛利率 */
    private BigDecimal profitMargin;
}
