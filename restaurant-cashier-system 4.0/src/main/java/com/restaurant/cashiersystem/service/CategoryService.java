package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.cashiersystem.entity.Category;
import java.util.List;

/**
 * 菜品分类 Service 接口
 */
public interface CategoryService extends IService<Category> {

    /**
     * 根据门店ID查询分类列表（仅未删除）
     * @param storeId 门店ID
     * @return 分类列表
     */
    List<Category> getCategoriesByStore(Long storeId);

    /**
     * 逻辑删除分类
     * @param id 分类ID
     */
    void deleteCategoryLogic(Long id);
}