package com.restaurant.cashiersystem.controller;

import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.Order;
import com.restaurant.cashiersystem.entity.OrderItem;
import com.restaurant.cashiersystem.service.OrderItemService;
import com.restaurant.cashiersystem.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "订单明细", description = "加菜、退菜、修改明细")
@RestController
@RequestMapping("/api/order-items")
public class OrderItemController {

    private static final Logger log = LoggerFactory.getLogger(OrderItemController.class);

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderService orderService;   // 用于查询订单信息

    @GetMapping("/order/{orderId}")
    public Result<List<OrderItem>> getItemsByOrderId(@PathVariable Long orderId) {
        List<OrderItem> list = orderItemService.getItemsByOrderId(orderId);
        return Result.success(list);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'waiter')")
    public Result<Long> addOrderItem(@RequestBody OrderItem orderItem) {
        // 自动补充 storeId（如果前端没有传）
        if (orderItem.getStoreId() == null && orderItem.getOrderId() != null) {
            Order order = orderService.getById(orderItem.getOrderId());
            if (order != null) {
                orderItem.setStoreId(order.getStoreId());
            }
        }
        // 自动设置默认状态为 active
        if (orderItem.getStatus() == null) {
            orderItem.setStatus("active");
        }
        orderItemService.save(orderItem);
        return Result.success(orderItem.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'waiter')")
    public Result<Void> updateOrderItem(@PathVariable Long id, @RequestBody OrderItem orderItem) {
        orderItem.setId(id);
        orderItemService.updateById(orderItem);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'waiter')")
    public Result<Void> deleteOrderItem(@PathVariable Long id) {
        orderItemService.removeById(id);
        return Result.success();
    }
}