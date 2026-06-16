package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.DishRecipe;
import com.restaurant.cashiersystem.mapper.DishRecipeMapper;
import com.restaurant.cashiersystem.service.DishRecipeService;
import org.springframework.stereotype.Service;

@Service
public class DishRecipeServiceImpl extends ServiceImpl<DishRecipeMapper, DishRecipe> implements DishRecipeService {
}