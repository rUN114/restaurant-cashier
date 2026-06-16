package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.Category;
import com.restaurant.cashiersystem.mapper.CategoryMapper;
import com.restaurant.cashiersystem.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜品分类 Service 实现类
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    /**
     * 根据门店ID查询分类列表
     */
    @Override
    public List<Category> getCategoriesByStore(Long storeId) {
        QueryWrapper<Category> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId);
        // 去掉 deleted 查询，避免报错
        wrapper.orderByAsc("sort_order");
        return this.list(wrapper);
    }

    /**
     * 逻辑删除分类（直接删除，兼容你的表）
     */
    @Override
    public void deleteCategoryLogic(Long id) {
        this.removeById(id);
    }
}