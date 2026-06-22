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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    @Transactional(rollbackFor = Exception.class)
    public void consumeOrderMaterials(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> orderItems = orderItemService.list(wrapper);

        // 第零步：一趟遍历加载所有食谱并收集涉及的原料 ID，批量加载原料名称
        // 替代内层循环中逐条 getById() 的 N+1 查询
        Set<Long> materialIdSet = new HashSet<>();
        List<OrderItemRecipe> itemRecipes = new ArrayList<>();

        for (OrderItem item : orderItems) {
            LambdaQueryWrapper<DishRecipe> rWrapper = new LambdaQueryWrapper<>();
            rWrapper.eq(DishRecipe::getDishId, item.getDishId())
                    .eq(DishRecipe::getStoreId, item.getStoreId());
            List<DishRecipe> recipes = dishRecipeService.list(rWrapper);
            itemRecipes.add(new OrderItemRecipe(item, recipes));
            recipes.forEach(r -> materialIdSet.add(r.getMaterialId()));
        }

        Map<Long, String> materialNameMap = new HashMap<>();
        if (!materialIdSet.isEmpty()) {
            materialService.listByIds(materialIdSet)
                    .forEach(m -> materialNameMap.put(m.getId(), m.getName()));
        }

        // 第一步：SELECT ... FOR UPDATE 行锁查询库存并预校验
        List<String> insufficientItems = new ArrayList<>();
        List<ItemDeduction> deductions = new ArrayList<>();

        for (OrderItemRecipe ir : itemRecipes) {
            OrderItem item = ir.item;
            Long dishId = item.getDishId();
            Long storeId = item.getStoreId();
            int num = item.getQuantity();

            String dishName = item.getDishName();
            if (dishName == null || dishName.isEmpty()) {
                Dish dish = dishService.getById(dishId);
                dishName = dish != null ? dish.getName() : ("菜品#" + dishId);
            }

            List<DishRecipe> recipes = ir.recipes;
            if (recipes.isEmpty()) {
                insufficientItems.add("【" + dishName + "】未配置配方，请先在菜品配方中为该菜品添加原料");
                continue;
            }

            for (DishRecipe recipe : recipes) {
                BigDecimal required = recipe.getUsedQuantity().multiply(new BigDecimal(num));

                // O(1) Map 查原料名，替代逐条 getById()
                String materialName = materialNameMap.getOrDefault(
                        recipe.getMaterialId(), "原料#" + recipe.getMaterialId());

                MaterialInventory inv = materialInventoryService.lambdaQuery()
                        .eq(MaterialInventory::getStoreId, storeId)
                        .eq(MaterialInventory::getMaterialId, recipe.getMaterialId())
                        .last("FOR UPDATE")
                        .one();

                if (inv == null) {
                    insufficientItems.add("【" + dishName + "】缺少原料「" + materialName + "」的库存记录");
                } else if (inv.getCurrentStock().compareTo(required) < 0) {
                    insufficientItems.add("【" + dishName + "】需要原料「" + materialName
                            + "」" + required + "，当前库存仅" + inv.getCurrentStock());
                } else {
                    deductions.add(new ItemDeduction(inv, required, orderId, storeId, dishId,
                            recipe.getMaterialId()));
                }
            }
        }

        if (!insufficientItems.isEmpty()) {
            String msg = "库存不足，无法结账：" + String.join("; ", insufficientItems);
            log.warn("订单 {} 结账失败：{}", orderId, msg);
            throw new RuntimeException(msg);
        }

        // 第二步：批量扣减与消费记录（同一事务内，FOR UPDATE 锁持续持有）
        for (ItemDeduction d : deductions) {
            d.inv.setCurrentStock(d.inv.getCurrentStock().subtract(d.amount));
            materialInventoryService.updateById(d.inv);

            MaterialConsumption c = new MaterialConsumption();
            c.setStoreId(d.storeId);
            c.setOrderId(d.orderId);
            c.setDishId(d.dishId);
            c.setMaterialId(d.materialId);
            c.setUsedQuantity(d.amount);
            save(c);
        }

        log.info("订单 {} 原材料消耗扣减完成，共扣减 {} 条", orderId, deductions.size());
    }

    /** 订单项与其配方的绑定，用于一趟遍历收集 */
    private record OrderItemRecipe(OrderItem item, List<DishRecipe> recipes) {}

    /** 待扣减条目：持有已加锁的 inventory 对象，避免二次查询 */
    private record ItemDeduction(MaterialInventory inv, BigDecimal amount, Long orderId,
                                 Long storeId, Long dishId, Long materialId) {}
}