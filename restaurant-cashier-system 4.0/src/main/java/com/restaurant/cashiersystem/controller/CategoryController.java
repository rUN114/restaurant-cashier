package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.Category;
import com.restaurant.cashiersystem.service.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "分类管理", description = "菜品分类 CRUD，按门店查询")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private CategoryService categoryService;

    /**
     * 根据门店ID查询菜品分类列表（按 sort_order 升序）
     */
    @GetMapping("/store/{storeId}")
    public Result<List<Category>> getCategoriesByStore(@PathVariable Long storeId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getStoreId, storeId)
                .orderByAsc(Category::getSortOrder);
        List<Category> list = categoryService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 新增菜品分类
     * 权限：管理员 + 店长
     * 自动计算排序值（当前门店最大排序值 + 1）
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Long> createCategory(@RequestBody Category category) {
        // 如果前端没有传 sortOrder，则自动计算
        if (category.getSortOrder() == null) {
            LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Category::getStoreId, category.getStoreId())
                    .orderByDesc(Category::getSortOrder)
                    .last("LIMIT 1");
            Category last = categoryService.getOne(wrapper);
            int nextOrder = (last != null && last.getSortOrder() != null) ? last.getSortOrder() + 1 : 1;
            category.setSortOrder(nextOrder);
        }
        categoryService.save(category);
        return Result.success(category.getId());
    }

    /**
     * 修改菜品分类（支持修改排序值）
     * 权限：管理员 + 店长
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryService.updateById(category);
        return Result.success();
    }

    /**
     * 查询所有分类（仅管理员）
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('admin_global')")
    public Result<List<Category>> getAllCategories() {
        List<Category> list = categoryService.list(); // 自动过滤软删除
        return Result.success(list);
    }

    /**
     * 逻辑删除菜品分类
     * 权限：管理员 + 店长
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategoryLogic(id);
        return Result.success();
    }
}