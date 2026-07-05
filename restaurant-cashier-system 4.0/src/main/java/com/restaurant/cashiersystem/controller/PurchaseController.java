package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.common.PageResult;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.Material;
import com.restaurant.cashiersystem.entity.MaterialInventory;
import com.restaurant.cashiersystem.entity.Purchase;
import com.restaurant.cashiersystem.service.MaterialInventoryService;
import com.restaurant.cashiersystem.service.MaterialService;
import com.restaurant.cashiersystem.service.PurchaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "采购管理", description = "采购入库（自动更新库存）及查询")
@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {
    private static final Logger log = LoggerFactory.getLogger(PurchaseController.class);

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private MaterialInventoryService inventoryService;

    @Autowired
    private MaterialService materialService;

    // ✅ 修复：添加admin_global权限
    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'purchaser')")
    public Result<List<Purchase>> getPurchasesByStore(@PathVariable Long storeId) {
        LambdaQueryWrapper<Purchase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Purchase::getStoreId, storeId);
        wrapper.orderByDesc(Purchase::getPurchaseTime);
        return Result.success(purchaseService.list(wrapper));
    }

    /** 采购入库 — 自动更新库存（含单位换算 + 移动加权平均 + 行锁） */
    @PostMapping
    @Transactional
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'purchaser')")
    public Result<Long> createPurchase(@RequestBody Purchase purchase) {
        // 0. 输入校验
        if (purchase.getQuantity() == null || purchase.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.badRequest("采购数量必须大于0");
        }
        if (purchase.getUnitPrice() == null || purchase.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            return Result.badRequest("采购单价不能为空或负数");
        }
        if (purchase.getPurchaseTime() == null) {
            purchase.setPurchaseTime(LocalDateTime.now());
        }
        // 默认 totalPrice
        if (purchase.getTotalPrice() == null) {
            purchase.setTotalPrice(purchase.getUnitPrice().multiply(purchase.getQuantity()));
        }

        // 1. 单位换算：采购单位 → 库存单位
        BigDecimal unitRate = BigDecimal.ONE;
        BigDecimal purchaseQty = purchase.getQuantity();   // 采购单位数量
        BigDecimal stockQty = purchase.getQuantity();       // 库存单位数量
        Material material = materialService.getById(purchase.getMaterialId());
        if (material != null && material.getUnitRate() != null
                && material.getUnitRate().compareTo(BigDecimal.ZERO) > 0) {
            unitRate = material.getUnitRate();
            stockQty = purchase.getQuantity().multiply(unitRate);
            log.info("采购单位换算：materialId={}, {}采购单位 × {} = {}库存单位",
                    purchase.getMaterialId(), purchaseQty, unitRate, stockQty);
        }

        // 2. 保存采购记录
        purchaseService.save(purchase);

        // 3. 行锁查询库存（FOR UPDATE 防并发丢更新）
        MaterialInventory inventory = inventoryService.lambdaQuery()
                .eq(MaterialInventory::getStoreId, purchase.getStoreId())
                .eq(MaterialInventory::getMaterialId, purchase.getMaterialId())
                .last("FOR UPDATE")
                .one();

        if (inventory == null) {
            // 新建库存记录（成本价按库存单位存储）
            inventory = new MaterialInventory();
            inventory.setStoreId(purchase.getStoreId());
            inventory.setMaterialId(purchase.getMaterialId());
            inventory.setCurrentStock(stockQty);
            // costPrice: 采购单位价 ÷ unitRate = 库存单位价
            BigDecimal inventoryUnitPrice = purchase.getUnitPrice()
                    .divide(unitRate, 4, RoundingMode.HALF_UP);
            inventory.setCostPrice(inventoryUnitPrice);
        } else {
            // 移动加权平均：先保存旧值再计算（修复：旧库存×旧成本价 而非 新库存×旧成本价）
            BigDecimal oldStock = inventory.getCurrentStock();
            BigDecimal oldCostPrice = inventory.getCostPrice() != null ? inventory.getCostPrice() : BigDecimal.ZERO;
            BigDecimal oldTotalCost = oldStock.multiply(oldCostPrice);
            // 新采购总成本 = 采购数量(采购单位) × 采购单价
            BigDecimal newTotalCost = purchaseQty.multiply(purchase.getUnitPrice());
            // 新总库存 = 旧库存 + 采购量(换算为库存单位)
            BigDecimal newTotalStock = oldStock.add(stockQty);
            if (newTotalStock.compareTo(BigDecimal.ZERO) > 0) {
                inventory.setCostPrice(oldTotalCost.add(newTotalCost)
                        .divide(newTotalStock, 4, RoundingMode.HALF_UP));
            }
            inventory.setCurrentStock(newTotalStock);
        }

        inventoryService.saveOrUpdate(inventory);
        log.info("采购入库完成：materialId={}, 采购{}采购单位 → 库存+{}库存单位, 新成本价={}",
                purchase.getMaterialId(), purchaseQty, stockQty, inventory.getCostPrice());
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
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String materialName) {
        LambdaQueryWrapper<Purchase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Purchase::getStoreId, storeId);

        // 按原料名称模糊搜索
        if (materialName != null && !materialName.isBlank()) {
            LambdaQueryWrapper<Material> matWrapper = new LambdaQueryWrapper<>();
            matWrapper.like(Material::getName, materialName);
            List<Long> materialIds = materialService.list(matWrapper).stream()
                    .map(Material::getId)
                    .collect(Collectors.toList());
            if (materialIds.isEmpty()) {
                return PageResult.success(Collections.emptyList(), 0L, 0,
                        1, pageSize);
            }
            wrapper.in(Purchase::getMaterialId, materialIds);
        }

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