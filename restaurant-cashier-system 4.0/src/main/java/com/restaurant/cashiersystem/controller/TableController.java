package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.Table;
import com.restaurant.cashiersystem.service.TableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 餐桌管理控制器
 */
@Tag(name = "餐桌管理", description = "餐桌 CRUD、状态切换、按门店/空闲查询")
@RestController
@RequestMapping("/api/tables")
public class TableController {

    private static final Logger log = LoggerFactory.getLogger(TableController.class);

    @Autowired
    private TableService tableService;


    /**
     * 查询所有餐桌（仅管理员，自动过滤软删除）
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('admin_global')")
    public Result<List<Table>> getAllTables() {
        LambdaQueryWrapper<Table> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Table::getTableNumber);   // 按桌号排序
        List<Table> list = tableService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 根据门店ID查询餐桌列表
     * @param storeId 门店ID
     * @return 餐桌列表
     */
    @GetMapping("/store/{storeId}")
    public Result<List<Table>> getTablesByStore(@PathVariable Long storeId) {
        LambdaQueryWrapper<Table> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Table::getStoreId, storeId)
                .orderByAsc(Table::getTableNumber);
        List<Table> list = tableService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 根据门店ID查询空闲餐桌
     * @param storeId 门店ID
     * @return 空闲餐桌列表
     */
    @GetMapping("/free/{storeId}")
    public Result<List<Table>> getFreeTables(@PathVariable Long storeId) {
        List<Table> list = tableService.getFreeTables(storeId);
        return Result.success(list);
    }

    /**
     * 修改餐桌状态
     * 权限：管理员 + 店长 + 服务员（开台/结账换状态）
     * @param tableId 餐桌ID
     * @param status 餐桌状态
     * @return 无
     */
    @PutMapping("/status/{tableId}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'waiter', 'cashier')")
    public Result<Void> updateTableStatus(@PathVariable Long tableId, @RequestParam String status) {
        if (!"free".equals(status) && !"occupied".equals(status)) {
            return Result.error("无效状态，仅支持 free 或 occupied");
        }
        tableService.updateTableStatus(tableId, status);
        return Result.success();
    }

    /**
     * 新增餐桌
     * 权限：管理员 + 店长
     * @param table 餐桌信息
     * @return 餐桌ID
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Long> createTable(@RequestBody Table table) {
        tableService.save(table);
        return Result.success(table.getId());
    }

    /**
     * 修改餐桌信息（桌号、区域、容量、状态）
     * 权限：管理员 + 店长
     * @param id 餐桌ID
     * @param table 餐桌信息
     * @return 无
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Void> updateTable(@PathVariable Long id, @RequestBody Table table) {
        table.setId(id);
        tableService.updateById(table);
        return Result.success();
    }

    /**
     * 删除餐桌
     * 权限：只有管理员能删
     * @param id 餐桌ID
     * @return 无
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin_global')")
    public Result<Void> deleteTable(@PathVariable Long id) {
        tableService.removeById(id);
        return Result.success();
    }
}