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
     * 库存调整（进货/出库），自动生成记录
     */
    @PostMapping("/adjust")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','purchaser')")
    @Transactional
    @Operation(summary = "调整库存", description = "quantity 正数为进货，负数为出库")
    public Result<Void> adjustInventory(@RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        Long storeId = Long.valueOf(body.get("storeId").toString());
        Long materialId = Long.valueOf(body.get("materialId").toString());
        BigDecimal quantity = new BigDecimal(body.get("quantity").toString());
        Long operatorId = getUserIdFromToken(request);

        if (quantity.compareTo(BigDecimal.ZERO) == 0) {
            return Result.badRequest("调整数量不能为0");
        }

        boolean isStockIn = quantity.compareTo(BigDecimal.ZERO) > 0;

        LambdaQueryWrapper<MaterialInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialInventory::getStoreId, storeId);
        wrapper.eq(MaterialInventory::getMaterialId, materialId);
        MaterialInventory inv = materialInventoryService.getOne(wrapper);

        BigDecimal beforeStock;
        BigDecimal afterStock;

        if (inv == null) {
            if (isStockIn) {
                beforeStock = BigDecimal.ZERO;
                afterStock = quantity;
                inv = new MaterialInventory();
                inv.setStoreId(storeId);
                inv.setMaterialId(materialId);
                inv.setCurrentStock(afterStock);
                materialInventoryService.save(inv);
                log.info("新建库存记录：storeId={}, materialId={}, stock={}", storeId, materialId, quantity);
            } else {
                return Result.badRequest("该原料无库存记录，不能出库");
            }
        } else {
            beforeStock = inv.getCurrentStock();
            afterStock = beforeStock.add(quantity);
            if (afterStock.compareTo(BigDecimal.ZERO) < 0) {
                return Result.badRequest("库存不足，当前库存=" + beforeStock + "，出库=" + quantity.abs());
            }
            inv.setCurrentStock(afterStock);
            materialInventoryService.updateById(inv);
            log.info("调整库存：storeId={}, materialId={}, 原库存={}, 调整量={}, 新库存={}",
                    storeId, materialId, beforeStock, quantity, afterStock);
        }

        // 记录出入库流水
        StockRecord record = new StockRecord();
        record.setStoreId(storeId);
        record.setMaterialId(materialId);
        record.setType(isStockIn ? "in" : "out");
        record.setQuantity(quantity.abs());
        record.setBeforeStock(beforeStock);
        record.setAfterStock(afterStock);
        record.setOperatorId(operatorId);
        record.setRemark(body.get("remark") != null ? body.get("remark").toString() : null);
        stockRecordService.save(record);
        log.info("库存记录已保存：type={}, materialId={}, {} -> {}", record.getType(), materialId, beforeStock, afterStock);

        // 进货时同步生成采购入库记录
        if (isStockIn) {
            BigDecimal unitPrice = null;
            try {
                if (body.get("unitPrice") != null) {
                    unitPrice = new BigDecimal(body.get("unitPrice").toString());
                }
            } catch (Exception ignored) {}
            if (unitPrice == null) unitPrice = BigDecimal.ZERO;

            Purchase purchase = new Purchase();
            purchase.setStoreId(storeId);
            purchase.setMaterialId(materialId);
            purchase.setQuantity(quantity);
            purchase.setUnitPrice(unitPrice);
            purchase.setTotalPrice(unitPrice.multiply(quantity));
            purchase.setPurchaseTime(java.time.LocalDateTime.now());
            purchase.setSupplier(body.get("supplier") != null ? body.get("supplier").toString() : null);
            purchaseService.save(purchase);
            log.info("同步生成采购记录：materialId={}, quantity={}, unitPrice={}", materialId, quantity, unitPrice);
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
     * 低库存预警（当前库存 < 预警值）
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','chef','purchaser')")
    @Operation(summary = "低库存预警", description = "返回库存低于预警值的原料")
    public Result<List<Map<String, Object>>> getLowStock(@RequestParam Long storeId) {
        LambdaQueryWrapper<MaterialInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialInventory::getStoreId, storeId);
        List<MaterialInventory> all = materialInventoryService.list(wrapper);

        var materialWrapper = new LambdaQueryWrapper<com.restaurant.cashiersystem.entity.Material>();
        materialWrapper.eq(com.restaurant.cashiersystem.entity.Material::getStoreId, storeId);
        List<com.restaurant.cashiersystem.entity.Material> materials = materialService.list(materialWrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (MaterialInventory inv : all) {
            com.restaurant.cashiersystem.entity.Material mat = materials.stream()
                    .filter(m -> m.getId().equals(inv.getMaterialId()))
                    .findFirst().orElse(null);
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
