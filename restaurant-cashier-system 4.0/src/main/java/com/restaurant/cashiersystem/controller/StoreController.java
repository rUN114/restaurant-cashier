package com.restaurant.cashiersystem.controller;

import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.Store;
import com.restaurant.cashiersystem.service.StoreService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分店管理控制器
 */
@Tag(name = "门店管理", description = "门店 CRUD、有效门店查询")
@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private static final Logger log = LoggerFactory.getLogger(StoreController.class);

    @Autowired
    private StoreService storeService;

    /**
     * 查询有效（正常营业）的门店列表
     * @return 门店列表
     */
    @GetMapping("/valid")
    @Cacheable(value = "stores", key = "'valid'")
    public Result<List<Store>> getValidStores() {
        List<Store> list = storeService.getValidStores();
        return Result.success(list);
    }

    /**
     * 查询所有门店（包括停业，但不包括已软删除的）
     * 权限：管理员 + 店长
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<List<Store>> getAllStores() {
        List<Store> list = storeService.list();   // MyBatis-Plus 的 list() 自动过滤软删除
        return Result.success(list);
    }

    /**
     * 根据ID查询门店详情
     * @param id 门店ID
     * @return 门店信息
     */
    @GetMapping("/{id}")
    public Result<Store> getStoreById(@PathVariable Long id) {
        Store store = storeService.getById(id);
        return Result.success(store);
    }

    /**
     * 新增门店
     * 权限：管理员 + 店长
     * @param store 门店信息
     * @return 门店ID
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    @CacheEvict(value = "stores", allEntries = true)
    public Result<Long> createStore(@RequestBody Store store) {
        storeService.save(store);
        return Result.success(store.getId());
    }

    /**
     * 修改门店信息
     * 权限：管理员 + 店长
     * @param id 门店ID
     * @param store 门店信息
     * @return 无
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    @CacheEvict(value = "stores", allEntries = true)
    public Result<Void> updateStore(@PathVariable Long id, @RequestBody Store store) {
        store.setId(id);
        storeService.updateById(store);
        return Result.success();
    }

    /**
     * 逻辑删除门店
     * 权限：只有管理员能删
     * @param id 门店ID
     * @return 无
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin_global')")
    @CacheEvict(value = "stores", allEntries = true)
    public Result<Void> deleteStore(@PathVariable Long id) {
        storeService.deleteStoreLogic(id);
        return Result.success();
    }
}