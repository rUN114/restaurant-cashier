package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.cashiersystem.entity.OrderItemLog;
import java.util.List;

/**
 * 菜品操作流水接口（加菜/退菜/改量日志）
 */
public interface OrderItemLogService extends IService<OrderItemLog> {

    /**
     * 查询订单的所有操作记录
     */
    List<OrderItemLog> getLogsByOrderId(Long orderId);
}