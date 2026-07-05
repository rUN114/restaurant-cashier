package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.MaterialInventory;
import com.restaurant.cashiersystem.entity.Purchase;
import com.restaurant.cashiersystem.entity.StockRecord;
import com.restaurant.cashiersystem.service.MaterialInventoryService;
import com.restaurant.cashiersystem.service.MaterialService;
import com.restaurant.cashiersystem.service.PurchaseService;
import com.restaurant.cashiersystem.service.StockRecordService;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Tag(name = "库存管理", description = "按门店查询原料库存、调整库存")
@RestController
@RequestMapping("/api/inventory")
public class MaterialInventoryController {
    private static final Logger log = LoggerFactory.getLogger(MaterialInventoryController.class);

    @Autowired
    private MaterialInventoryService materialInventoryService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private StockRecordService stockRecordService;

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store', 'chef', 'purchaser')")
    public Result<List<MaterialInventory>> getInventoryByStore(@PathVariable Long storeId) {
        LambdaQueryWrapper<MaterialInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialInventory::getStoreId, storeId);
        List<MaterialInventory> list = materialInventoryService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 库存调整（进货/出库），自动生成出入库记录。
     * 进货时 quantity 为采购单位数量，后端按 unit_rate 换算成库存单位。
     * 出库时 quantity 直接为库存单位。
     */
    @PostMapping("/adjust")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','purchaser')")
    @Transactional
    @Operation(summary = "调整库存", description = "进货quantity=采购单位数量，出库quantity=库存单位数量")
    public Result<Void> adjustInventory(@RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        Long storeId = Long.valueOf(body.get("storeId").toString());
        Long materialId = Long.valueOf(body.get("materialId").toString());
        BigDecimal quantity = new BigDecimal(body.get("quantity").toString());
        Long operatorId = getUserIdFromToken(request);

        if (operatorId == null) {
            return Result.unauthorized("无法获取操作人信息，请重新登录");
        }
        if (quantity.compareTo(BigDecimal.ZERO) == 0) {
            return Result.badRequest("调整数量不能为0");
        }

        boolean isStockIn = quantity.compareTo(BigDecimal.ZERO) > 0;

        // 进货：查原料采购单位换算率
        BigDecimal unitRate = BigDecimal.ONE;
        BigDecimal purchaseQty = quantity;
        BigDecimal stockQty = quantity;
        if (isStockIn) {
            var material = materialService.getById(materialId);
            if (material != null && material.getUnitRate() != null
                    && material.getUnitRate().compareTo(BigDecimal.ZERO) > 0) {
                unitRate = material.getUnitRate();
                stockQty = quantity.multiply(unitRate);
                log.info("进货单位换算：materialId={}, {}采购单位 × {} = {}库存单位",
                        materialId, purchaseQty, unitRate, stockQty);
            }
        }

        // 行锁查询（FOR UPDATE 防并发丢更新）
        MaterialInventory inv = materialInventoryService.lambdaQuery()
                .eq(MaterialInventory::getStoreId, storeId)
                .eq(MaterialInventory::getMaterialId, materialId)
                .last("FOR UPDATE")
                .one();

        BigDecimal beforeStock;
        BigDecimal afterStock;
        BigDecimal unitPrice = null;  // 采购单位价

        if (isStockIn) {
            try {
                if (body.get("unitPrice") != null) {
                    unitPrice = new BigDecimal(body.get("unitPrice").toString());
                }
            } catch (Exception e) {
                log.warn("unitPrice 解析失败: {}", body.get("unitPrice"), e);
            }
            if (unitPrice == null) unitPrice = BigDecimal.ZERO;
        }

        if (inv == null) {
            if (isStockIn) {
                beforeStock = BigDecimal.ZERO;
                afterStock = stockQty;
                inv = new MaterialInventory();
                inv.setStoreId(storeId);
                inv.setMaterialId(materialId);
                inv.setCurrentStock(afterStock);
                // 库存单位成本价 = 采购单位价 / unitRate
                BigDecimal inventoryUnitPrice = unitRate.compareTo(BigDecimal.ZERO) > 0
                        ? unitPrice.divide(unitRate, 4, RoundingMode.HALF_UP) : unitPrice;
                inv.setCostPrice(inventoryUnitPrice);
                materialInventoryService.save(inv);
                log.info("新建库存记录：storeId={}, materialId={}, stock={}, costPrice={}",
                        storeId, materialId, stockQty, inventoryUnitPrice);
            } else {
                return Result.badRequest("该原料无库存记录，不能出库");
            }
        } else {
            beforeStock = inv.getCurrentStock() != null ? inv.getCurrentStock() : BigDecimal.ZERO;
            afterStock = beforeStock.add(stockQty);
            if (afterStock.compareTo(BigDecimal.ZERO) < 0) {
                return Result.badRequest("库存不足，当前库存=" + beforeStock + "，出库=" + stockQty.abs());
            }
            inv.setCurrentStock(afterStock);
            // 进货时：移动加权平均计算库存单位成本价
            if (isStockIn && unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal oldCostPrice = inv.getCostPrice() != null ? inv.getCostPrice() : BigDecimal.ZERO;
                BigDecimal oldTotalCost = beforeStock.multiply(oldCostPrice);
                BigDecimal newTotalCost = purchaseQty.multiply(unitPrice);
                BigDecimal totalStock = afterStock;
                if (totalStock.compareTo(BigDecimal.ZERO) > 0) {
                    inv.setCostPrice(oldTotalCost.add(newTotalCost)
                            .divide(totalStock, 4, RoundingMode.HALF_UP));
                }
            }
            materialInventoryService.updateById(inv);
            log.info("调整库存：storeId={}, materialId={}, 原库存={}, 调整量={}, 新库存={}",
                    storeId, materialId, beforeStock, stockQty, afterStock);
        }

        // 记录出入库流水（库存单位）
        StockRecord record = new StockRecord();
        record.setStoreId(storeId);
        record.setMaterialId(materialId);
        record.setType(isStockIn ? "in" : "out");
        record.setQuantity(stockQty.abs());
        record.setBeforeStock(beforeStock);
        record.setAfterStock(afterStock);
        record.setOperatorId(operatorId);
        record.setRemark(body.get("remark") != null ? body.get("remark").toString() : null);
        stockRecordService.save(record);
        log.info("库存记录已保存：type={}, materialId={}, {} -> {}", record.getType(), materialId, beforeStock, afterStock);

        // 进货时同步生成采购入库记录（采购单位）
        if (isStockIn) {
            Purchase purchase = new Purchase();
            purchase.setStoreId(storeId);
            purchase.setMaterialId(materialId);
            purchase.setQuantity(purchaseQty);
            purchase.setUnitPrice(unitPrice);
            purchase.setTotalPrice(unitPrice.multiply(purchaseQty));
            purchase.setPurchaseTime(java.time.LocalDateTime.now());
            purchase.setSupplier(body.get("supplier") != null ? body.get("supplier").toString() : null);
            purchaseService.save(purchase);
            log.info("同步生成采购记录：materialId={}, purchaseQty={}{}, unitPrice={}", materialId, purchaseQty,
                    unitRate.compareTo(BigDecimal.ONE) != 0 ? "（采购单位）" : "", unitPrice);
        }

        return Result.success();
    }

    /**
     * 出入库记录列表
     */
    @GetMapping("/records")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','purchaser')")
    public Result<List<StockRecord>> getRecords(@RequestParam Long storeId) {
        return Result.success(stockRecordService.getByStoreId(storeId));
    }

    /**
     * 查询某原料最近 N 次采购价（进货时参考）
     */
    @GetMapping("/purchase-history")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','purchaser')")
    public Result<List<Map<String, Object>>> getPurchaseHistory(@RequestParam Long storeId,
                                                                 @RequestParam Long materialId) {
        var wrapper = new LambdaQueryWrapper<Purchase>();
        wrapper.eq(Purchase::getStoreId, storeId)
               .eq(Purchase::getMaterialId, materialId)
               .orderByDesc(Purchase::getPurchaseTime)
               .last("LIMIT 3");
        List<Purchase> purchases = purchaseService.list(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Purchase p : purchases) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("unitPrice", p.getUnitPrice());
            item.put("quantity", p.getQuantity());
            item.put("supplier", p.getSupplier());
            item.put("purchaseTime", p.getPurchaseTime() != null ? p.getPurchaseTime().toString() : null);
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 低库存预警（当前库存 < 预警值）
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','chef','purchaser')")
    @Operation(summary = "低库存预警", description = "返回库存低于预警值的原料")
    public Result<List<Map<String, Object>>> getLowStock(@RequestParam(required = false) Long storeId) {
        LambdaQueryWrapper<MaterialInventory> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null) wrapper.eq(MaterialInventory::getStoreId, storeId);
        List<MaterialInventory> all = materialInventoryService.list(wrapper);

        // HashMap 预构建索引 O(N+M) 替代 stream.filter O(N×M)
        var materialWrapper = new LambdaQueryWrapper<com.restaurant.cashiersystem.entity.Material>();
        if (storeId != null) materialWrapper.eq(com.restaurant.cashiersystem.entity.Material::getStoreId, storeId);
        List<com.restaurant.cashiersystem.entity.Material> materials = materialService.list(materialWrapper);
        Map<Long, com.restaurant.cashiersystem.entity.Material> materialMap = new HashMap<>();
        for (var m : materials) {
            materialMap.put(m.getId(), m);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (MaterialInventory inv : all) {
            if (inv.getCurrentStock() == null) continue;  // null 保护
            var mat = materialMap.get(inv.getMaterialId());  // O(1) Map 查找
            if (mat != null && mat.getWarningStock() != null
                    && mat.getWarningStock().compareTo(BigDecimal.ZERO) > 0
                    && inv.getCurrentStock().compareTo(mat.getWarningStock()) < 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("inventoryId", inv.getId());
                item.put("storeId", inv.getStoreId());
                item.put("materialId", inv.getMaterialId());
                item.put("currentStock", inv.getCurrentStock());
                item.put("name", mat.getName());
                item.put("unit", mat.getUnit());
                item.put("warningStock", mat.getWarningStock());
                result.add(item);
            }
        }
        log.debug("低库存查询 storeId={}, 结果数={}", storeId, result.size());
        return Result.success(result);
    }

    /**
     * 一键回填成本价：从最近一次采购记录取单价写入 material_inventory.cost_price
     * 仅更新 costPrice 为空的记录
     */
    @PostMapping("/backfill-cost-price")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    @Operation(summary = "回填库存成本价", description = "用最近一次采购单价填充空缺的成本价")
    public Result<Map<String, Object>> backfillCostPrice() {
        List<MaterialInventory> all = materialInventoryService.list();
        int updated = 0;
        int skipped = 0;

        for (MaterialInventory inv : all) {
            if (inv.getCostPrice() != null && inv.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
                skipped++;
                continue;
            }
            // 取最近一次有单价的采购记录
            var pw = new LambdaQueryWrapper<Purchase>();
            pw.eq(Purchase::getStoreId, inv.getStoreId())
              .eq(Purchase::getMaterialId, inv.getMaterialId())
              .gt(Purchase::getUnitPrice, BigDecimal.ZERO)
              .orderByDesc(Purchase::getPurchaseTime)
              .last("LIMIT 1");
            Purchase last = purchaseService.getOne(pw);
            if (last != null) {
                inv.setCostPrice(last.getUnitPrice());
                materialInventoryService.updateById(inv);
                updated++;
                log.info("回填成本价：storeId={}, materialId={}, costPrice={}", inv.getStoreId(), inv.getMaterialId(), last.getUnitPrice());
            } else {
                skipped++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", all.size());
        result.put("updated", updated);
        result.put("skipped", skipped);
        log.info("成本价回填完成：total={}, updated={}, skipped={}", all.size(), updated, skipped);
        return Result.success(result);
    }

    private Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parseToken(authHeader.substring(7));
                return Long.valueOf(claims.getSubject());
            } catch (Exception e) {
                log.error("解析token失败", e);
            }
        }
        return null;
    }
}
