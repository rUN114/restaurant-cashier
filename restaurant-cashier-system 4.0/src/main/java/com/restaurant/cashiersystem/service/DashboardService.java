package com.restaurant.cashiersystem.service;

import com.restaurant.cashiersystem.dto.DashboardVO;

public interface DashboardService {
    DashboardVO getTodayDashboard(Long storeId);
}
