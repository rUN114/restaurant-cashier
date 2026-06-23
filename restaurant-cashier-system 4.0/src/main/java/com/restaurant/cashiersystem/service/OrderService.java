package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.cashiersystem.entity.Order;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService extends IService<Order> {

    List<Order> getOrdersByStore(Long storeId);

    List<Order> getInProgressOrders(Long storeId);

    /**
     * 订单结账（支持折扣抹零、会员积分）
     */
    void settleOrder(Long orderId, Long cashierId, String paymentMethod,
                     BigDecimal discountAmount, BigDecimal finalAmount,
                     Long memberId, boolean useMemberDiscount, int redeemPoints);

    /**
     * 取消进行中的订单（退单），释放桌台并记录操作日志
     */
    void cancelOrder(Long orderId, Long operatorId);

    /**
     * 反结账：将已结算订单回退为进行中，回滚库存消耗和会员积分
     */
    void reverseSettlement(Long orderId, Long operatorId);

    BigDecimal getStoreTurnover(Long storeId);

    BigDecimal getTodayTurnover(Long storeId);

    BigDecimal getMonthTurnover(Long storeId);
}