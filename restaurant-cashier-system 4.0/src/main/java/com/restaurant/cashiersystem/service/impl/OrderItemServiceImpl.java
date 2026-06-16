package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.OperationLog;
import com.restaurant.cashiersystem.entity.Order;
import com.restaurant.cashiersystem.entity.OrderItem;
import com.restaurant.cashiersystem.mapper.OrderItemMapper;
import com.restaurant.cashiersystem.service.OperationLogService;
import com.restaurant.cashiersystem.service.OrderItemService;
import com.restaurant.cashiersystem.service.OrderService;
import com.restaurant.cashiersystem.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {

    private final OrderService orderService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private SecurityUtils securityUtils;

    public OrderItemServiceImpl(@Lazy OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public List<OrderItem> getItemsByOrderId(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addDishToOrder(OrderItem orderItem) {
        checkOrderStatus(orderItem.getOrderId());
        boolean result = save(orderItem);

        // 记录日志
        try {
            Order order = orderService.getById(orderItem.getOrderId());
            OperationLog log = new OperationLog();
            log.setStoreId(order != null ? order.getStoreId() : null);
            log.setOperatorId(securityUtils.getCurrentUserId());
            log.setTargetType("order_item");
            log.setTargetId(orderItem.getId() != null ? orderItem.getId().toString() : "");
            log.setAction("add");
            log.setDetail("加菜：" + orderItem.getDishName() + " x" + orderItem.getQuantity());
            log.setCreatedAt(LocalDateTime.now());
            operationLogService.save(log);
        } catch (Exception e) {
            // 日志记录失败不影响主业务
        }

        return result;
    }

    @Override
    public boolean updateById(OrderItem orderItem) {
        checkOrderStatus(orderItem.getOrderId());
        OrderItem old = getById(orderItem.getId());
        boolean result = super.updateById(orderItem);

        // 记录日志（数量变更）
        try {
            Order order = orderService.getById(orderItem.getOrderId());
            OperationLog log = new OperationLog();
            log.setStoreId(order != null ? order.getStoreId() : null);
            log.setOperatorId(securityUtils.getCurrentUserId());
            log.setTargetType("order_item");
            log.setTargetId(orderItem.getId().toString());
            log.setAction("update");
            log.setDetail(String.format("修改数量：%s 从 %d 改为 %d",
                    orderItem.getDishName(), old != null ? old.getQuantity() : 0, orderItem.getQuantity()));
            log.setCreatedAt(LocalDateTime.now());
            operationLogService.save(log);
        } catch (Exception e) { /* ignore */ }

        return result;
    }

    public boolean removeById(Long id) {
        OrderItem item = getById(id);
        checkOrderStatus(item.getOrderId());
        boolean result = super.removeById(id);

        // 记录日志（退菜）
        try {
            Order order = orderService.getById(item.getOrderId());
            OperationLog log = new OperationLog();
            log.setStoreId(order != null ? order.getStoreId() : null);
            log.setOperatorId(securityUtils.getCurrentUserId());
            log.setTargetType("order_item");
            log.setTargetId(id.toString());
            log.setAction("delete");
            log.setDetail("退菜：" + item.getDishName() + " x" + item.getQuantity());
            log.setCreatedAt(LocalDateTime.now());
            operationLogService.save(log);
        } catch (Exception e) { /* ignore */ }

        return result;
    }

    private void checkOrderStatus(Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!"in_progress".equals(order.getStatus())) {
            throw new RuntimeException("操作失败：只有进行中的订单才能修改菜品！");
        }
    }
}