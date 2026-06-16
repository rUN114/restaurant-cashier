package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.*;
import com.restaurant.cashiersystem.mapper.MaterialConsumptionMapper;
import com.restaurant.cashiersystem.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class MaterialConsumptionServiceImpl extends ServiceImpl<MaterialConsumptionMapper, MaterialConsumption> implements MaterialConsumptionService {

    private static final Logger log = LoggerFactory.getLogger(MaterialConsumptionServiceImpl.class);

    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private DishRecipeService dishRecipeService;
    @Autowired
    private MaterialInventoryService materialInventoryService;
    @Autowired
    private DishService dishService;
    @Autowired
    private MaterialService materialService;

    @Override
    @Transactional
    public void consumeOrderMaterials(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> orderItems = orderItemService.list(wrapper);

        // 第一步：预校验所有原材料库存是否充足
        List<String> insufficientItems = new ArrayList<>();
        for (OrderItem item : orderItems) {
            Long dishId = item.getDishId();
            Long storeId = item.getStoreId();
            int num = item.getQuantity();

            // 获取菜品名称用于友好提示
            String dishName = item.getDishName();
            if (dishName == null || dishName.isEmpty()) {
                Dish dish = dishService.getById(dishId);
                dishName = dish != null ? dish.getName() : ("菜品#" + dishId);
            }

            LambdaQueryWrapper<DishRecipe> rWrapper = new LambdaQueryWrapper<>();
            rWrapper.eq(DishRecipe::getDishId, dishId)
                    .eq(DishRecipe::getStoreId, storeId);
            List<DishRecipe> recipes = dishRecipeService.list(rWrapper);

            if (recipes.isEmpty()) {
                insufficientItems.add("【" + dishName + "】未配置配方，请先在菜品配方中为该菜品添加原料");
                continue;
            }

            for (DishRecipe recipe : recipes) {
                BigDecimal required = recipe.getUsedQuantity().multiply(new BigDecimal(num));

                // 获取原料名称用于友好提示
                Material material = materialService.getById(recipe.getMaterialId());
                String materialName = material != null ? material.getName() : ("原料#" + recipe.getMaterialId());

                MaterialInventory inv = materialInventoryService.lambdaQuery()
                        .eq(MaterialInventory::getStoreId, storeId)
                        .eq(MaterialInventory::getMaterialId, recipe.getMaterialId())
                        .one();

                if (inv == null) {
                    // 该原材料没有库存记录
                    insufficientItems.add("【" + dishName + "】缺少原料「" + materialName + "」的库存记录");
                } else if (inv.getCurrentStock().compareTo(required) < 0) {
                    // 库存不足
                    insufficientItems.add("【" + dishName + "】需要原料「" + materialName
                            + "」" + required + "，当前库存仅" + inv.getCurrentStock());
                }
            }
        }

        // 第二步：库存不足则抛出异常，事务回滚
        if (!insufficientItems.isEmpty()) {
            String msg = "库存不足，无法结账：" + String.join("; ", insufficientItems);
            log.warn("订单 {} 结账失败：{}", orderId, msg);
            throw new RuntimeException(msg);
        }

        // 第三步：库存充足，执行扣减
        for (OrderItem item : orderItems) {
            Long dishId = item.getDishId();
            Long storeId = item.getStoreId();
            int num = item.getQuantity();

            LambdaQueryWrapper<DishRecipe> rWrapper = new LambdaQueryWrapper<>();
            rWrapper.eq(DishRecipe::getDishId, dishId);
            List<DishRecipe> recipes = dishRecipeService.list(rWrapper);

            for (DishRecipe recipe : recipes) {
                BigDecimal total = recipe.getUsedQuantity().multiply(new BigDecimal(num));

                MaterialInventory inv = materialInventoryService.lambdaQuery()
                        .eq(MaterialInventory::getStoreId, storeId)
                        .eq(MaterialInventory::getMaterialId, recipe.getMaterialId())
                        .one();

                inv.setCurrentStock(inv.getCurrentStock().subtract(total));
                materialInventoryService.updateById(inv);

                MaterialConsumption c = new MaterialConsumption();
                c.setStoreId(storeId);
                c.setOrderId(orderId);
                c.setDishId(dishId);
                c.setMaterialId(recipe.getMaterialId());
                c.setUsedQuantity(total);
                save(c);
            }
        }
        log.info("订单 {} 原材料消耗扣减完成", orderId);
    }
}