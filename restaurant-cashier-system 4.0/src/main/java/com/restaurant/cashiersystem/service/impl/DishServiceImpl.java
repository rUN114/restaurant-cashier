package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.Dish;
import com.restaurant.cashiersystem.mapper.DishMapper;
import com.restaurant.cashiersystem.service.DishService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Override
    public List<Dish> getDishesByStore(Long storeId) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getStoreId, storeId);
        wrapper.isNull(Dish::getDeletedAt);
        return list(wrapper);
    }

    @Override
    public List<Dish> getAvailableDishes(Long storeId) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getStoreId, storeId);
        wrapper.eq(Dish::getIsAvailable, 1);
        wrapper.isNull(Dish::getDeletedAt);
        return list(wrapper);
    }

    @Override
    @Transactional
    public boolean deleteDishLogic(Long dishId) {
        Dish dish = getById(dishId);
        if (dish == null) return false;
        dish.setDeletedAt(LocalDateTime.now());
        return updateById(dish);
    }
}