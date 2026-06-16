package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.cashiersystem.entity.Dish;
import java.util.List;

public interface DishService extends IService<Dish> {
    List<Dish> getDishesByStore(Long storeId);
    List<Dish> getAvailableDishes(Long storeId);
    boolean deleteDishLogic(Long dishId);
}