package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.common.PageResult;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.MaterialInventory;
import com.restaurant.cashiersystem.entity.Purchase;
import com.restaurant.cashiersystem.service.MaterialInventoryService;
import com.restaurant.cashiersystem.service.PurchaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@Tag(name = "采购管理", description = "采购入库（自动更新库存）及查询")
@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {
    private static final Logger log = LoggerFactory.getLogger(PurchaseController.class);

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private MaterialInventoryService inventoryService;

    // ✅ 修复：添加admin_global权限
    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'purchaser')")
    public Result<List<Purchase>> getPurchasesByStore(@PathVariable Long storeId) {
        LambdaQueryWrapper<Purchase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Purchase::getStoreId, storeId);
        wrapper.orderByDesc(Purchase::getPurchaseTime);
        return Result.success(purchaseService.list(wrapper));
    }

    // ✅ 新增：采购入库自动更新库存（事务保证）
    @PostMapping
    @Transactional
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'purchaser')")
    public Result<Long> createPurchase(@RequestBody Purchase purchase) {
        // 1. 保存采购记录
        purchaseService.save(purchase);

        // 2. 更新库存
        LambdaQueryWrapper<MaterialInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialInventory::getStoreId, purchase.getStoreId());
        wrapper.eq(MaterialInventory::getMaterialId, purchase.getMaterialId());

        MaterialInventory inventory = inventoryService.getOne(wrapper);
        if (inventory == null) {
            // 如果该原料没有库存记录，创建新记录
            inventory = new MaterialInventory();
            inventory.setStoreId(purchase.getStoreId());
            inventory.setMaterialId(purchase.getMaterialId());
            inventory.setCurrentStock(purchase.getQuantity());
            inventory.setCostPrice(purchase.getUnitPrice());
        } else {
            // 如果已有库存记录，增加库存
            inventory.setCurrentStock(inventory.getCurrentStock().add(purchase.getQuantity()));
            // 移动加权平均计算成本价
            BigDecimal totalCost = inventory.getCurrentStock().multiply(inventory.getCostPrice())
                    .add(purchase.getQuantity().multiply(purchase.getUnitPrice()));
            BigDecimal totalStock = inventory.getCurrentStock().add(purchase.getQuantity());
            inventory.setCostPrice(totalCost.divide(totalStock, 2, BigDecimal.ROUND_HALF_UP));
        }

        inventoryService.saveOrUpdate(inventory);
        return Result.success(purchase.getId());
    }

    /**
     * 分页查询采购记录（按门店）
     */
    @GetMapping("/page/{storeId}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'purchaser')")
    public PageResult<List<Purchase>> getPurchasesByStorePage(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        LambdaQueryWrapper<Purchase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Purchase::getStoreId, storeId);
        wrapper.orderByDesc(Purchase::getPurchaseTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Purchase> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        purchaseService.page(page, wrapper);

        return PageResult.success(page.getRecords(), page.getTotal(), page.getPages(),
            (int) page.getCurrent(), (int) page.getSize());
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('admin_global')")
    public Result<List<Purchase>> getAllPurchases() {
        LambdaQueryWrapper<Purchase> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Purchase::getPurchaseTime);
        return Result.success(purchaseService.list(wrapper));
    }
}