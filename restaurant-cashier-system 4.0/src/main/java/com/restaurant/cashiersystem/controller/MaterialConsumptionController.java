package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.restaurant.cashiersystem.common.PageResult;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.MaterialConsumption;
import com.restaurant.cashiersystem.service.MaterialConsumptionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "原料消耗", description = "按订单查询消耗、全局查询")
@RestController
@RequestMapping("/api/consumptions")
public class MaterialConsumptionController {
    private static final Logger log = LoggerFactory.getLogger(MaterialConsumptionController.class);

    @Autowired
    private MaterialConsumptionService materialConsumptionService;

    // 修复：添加admin_global权限
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'chef')")
    public Result<List<MaterialConsumption>> getConsumptionsByOrder(@PathVariable Long orderId) {
        LambdaQueryWrapper<MaterialConsumption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialConsumption::getOrderId, orderId);
        List<MaterialConsumption> list = materialConsumptionService.list(wrapper);
        return Result.success(list);
    }

    // 分页查询消耗记录（管理员用）
    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public PageResult<List<MaterialConsumption>> getAllConsumptions(
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        LambdaQueryWrapper<MaterialConsumption> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null) {
            wrapper.eq(MaterialConsumption::getStoreId, storeId);
        }
        // 按创建时间倒序，最新的在最前面
        wrapper.orderByDesc(MaterialConsumption::getCreatedAt);

        Page<MaterialConsumption> page = new Page<>(pageNum, pageSize);
        materialConsumptionService.page(page, wrapper);

        return PageResult.success(page.getRecords(), page.getTotal(), page.getPages(),
            (int) page.getCurrent(), (int) page.getSize());
    }
}