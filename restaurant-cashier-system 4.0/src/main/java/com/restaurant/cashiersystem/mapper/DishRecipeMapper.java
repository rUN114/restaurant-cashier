package com.restaurant.cashiersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.cashiersystem.entity.DishRecipe;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DishRecipeMapper extends BaseMapper<DishRecipe> {
}