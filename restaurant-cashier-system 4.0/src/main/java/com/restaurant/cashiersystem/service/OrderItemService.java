package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.cashiersystem.entity.OrderItem;
import java.util.List;

/**
 * 订单菜品明细接口
 * 功能：查询订单菜品、加菜、退菜、统计金额
 */
public interface OrderItemService extends IService<OrderItem> {

    /**
     * 根据订单ID查询所有菜品明细
     */
    List<OrderItem> getItemsByOrderId(Long orderId);

    /**
     * 向订单追加菜品
     */
    boolean addDishToOrder(OrderItem orderItem);
}