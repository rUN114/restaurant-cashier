package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.*;
import com.restaurant.cashiersystem.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "订单明细", description = "加菜、退菜、修改明细")
@RestController
@RequestMapping("/api/order-items")
public class OrderItemController {

    private static final Logger log = LoggerFactory.getLogger(OrderItemController.class);

    @Autowired private OrderItemService orderItemService;
    @Autowired private OrderService orderService;
    @Autowired private StoreService storeService;
    @Autowired private DishRecipeService dishRecipeService;
    @Autowired private MaterialInventoryService materialInventoryService;
    @Autowired private MaterialService materialService;

    @GetMapping("/order/{orderId}")
    public Result<List<OrderItem>> getItemsByOrderId(@PathVariable Long orderId) {
        return Result.success(orderItemService.getItemsByOrderId(orderId));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'waiter')")
    public Result<Long> addOrderItem(@RequestBody OrderItem orderItem) {
        // 自动补充 storeId
        if (orderItem.getStoreId() == null && orderItem.getOrderId() != null) {
            Order order = orderService.getById(orderItem.getOrderId());
            if (order != null) orderItem.setStoreId(order.getStoreId());
        }
        if (orderItem.getStatus() == null) orderItem.setStatus("active");

        // ========== 1. 店铺停业检查 ==========
        storeService.checkStoreActive(orderItem.getStoreId());

        // ========== 2. 原材料库存预校验 ==========
        if (orderItem.getDishId() != null && orderItem.getStoreId() != null) {
            checkMaterialStock(orderItem.getDishId(), orderItem.getStoreId(),
                               orderItem.getQuantity() != null ? orderItem.getQuantity() : 1,
                               orderItem.getDishName());
        }

        orderItemService.save(orderItem);
        return Result.success(orderItem.getId());
    }

    /**
     * 校验菜品所需原材料库存是否充足，不足则抛出异常并指明缺料名称。
     */
    private void checkMaterialStock(Long dishId, Long storeId, int quantity, String dishName) {
        LambdaQueryWrapper<DishRecipe> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishRecipe::getDishId, dishId).eq(DishRecipe::getStoreId, storeId);
        List<DishRecipe> recipes = dishRecipeService.list(wrapper);

        if (recipes.isEmpty()) return; // 未配置配方则跳过

        List<String> insufficient = new ArrayList<>();
        for (DishRecipe recipe : recipes) {
            BigDecimal required = recipe.getUsedQuantity().multiply(new BigDecimal(quantity));
            MaterialInventory inv = materialInventoryService.lambdaQuery()
                    .eq(MaterialInventory::getStoreId, storeId)
                    .eq(MaterialInventory::getMaterialId, recipe.getMaterialId())
                    .one();
            if (inv == null || inv.getCurrentStock().compareTo(required) < 0) {
                Material material = materialService.getById(recipe.getMaterialId());
                String name = material != null ? material.getName() : ("原料#" + recipe.getMaterialId());
                String stock = inv != null ? inv.getCurrentStock().toString() : "无库存";
                insufficient.add("「" + name + "」需要" + required + "，当前库存" + stock);
            }
        }
        if (!insufficient.isEmpty()) {
            String label = dishName != null ? dishName : ("菜品#" + dishId);
            throw new RuntimeException("「" + label + "」材料不足，无法下单：" + String.join("；", insufficient));
        }
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