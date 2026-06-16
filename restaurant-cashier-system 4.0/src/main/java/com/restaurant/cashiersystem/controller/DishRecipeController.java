package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.DishRecipe;
import com.restaurant.cashiersystem.service.DishRecipeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "菜品配方", description = "配方 CRUD、按菜品查询")
@RestController
@RequestMapping("/api/recipes")
public class DishRecipeController {
    private static final Logger log = LoggerFactory.getLogger(DishRecipeController.class);

    @Autowired
    private DishRecipeService dishRecipeService;

    @GetMapping("/dish/{dishId}")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store', 'chef')")
    public Result<List<DishRecipe>> getRecipesByDish(@PathVariable Long dishId) {
        LambdaQueryWrapper<DishRecipe> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishRecipe::getDishId, dishId);
        List<DishRecipe> list = dishRecipeService.list(wrapper);
        return Result.success(list);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Long> saveRecipe(@RequestBody DishRecipe dishRecipe) {
        dishRecipeService.saveOrUpdate(dishRecipe);
        return Result.success(dishRecipe.getId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Void> deleteRecipe(@PathVariable Long id) {
        dishRecipeService.removeById(id);
        return Result.success();
    }
}