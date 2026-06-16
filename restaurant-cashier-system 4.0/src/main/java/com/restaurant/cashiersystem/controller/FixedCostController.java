package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.FixedCost;
import com.restaurant.cashiersystem.service.FixedCostService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "固定成本", description = "房租、水电等固定成本 CRUD")
@RestController
@RequestMapping("/api/fixed_costs")
public class FixedCostController {
    private static final Logger log = LoggerFactory.getLogger(FixedCostController.class);

    @Autowired
    private FixedCostService fixedCostService;

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<List<FixedCost>> getCostsByStore(@PathVariable Long storeId) {
        LambdaQueryWrapper<FixedCost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FixedCost::getStoreId, storeId);
        List<FixedCost> list = fixedCostService.list(wrapper);
        return Result.success(list);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Long> createCost(@RequestBody FixedCost fixedCost) {
        fixedCostService.save(fixedCost);
        return Result.success(fixedCost.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Void> updateCost(@PathVariable Long id, @RequestBody FixedCost fixedCost) {
        fixedCost.setId(id);
        fixedCostService.updateById(fixedCost);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Void> deleteCost(@PathVariable Long id) {
        fixedCostService.removeById(id);
        return Result.success();
    }
}