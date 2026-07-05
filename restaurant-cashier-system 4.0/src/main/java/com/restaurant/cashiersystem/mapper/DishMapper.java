package com.restaurant.cashiersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.cashiersystem.entity.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    /**
     * 乐观锁扣减秒杀库存
     * @return 受影响行数（1=成功 0=版本冲突/库存不足）
     */
    @Update("UPDATE dishes SET seckill_stock = seckill_stock - 1, version = version + 1 " +
            "WHERE id = #{dishId} AND seckill_stock > 0 AND version = #{version}")
    int deductSeckillStock(@Param("dishId") Long dishId, @Param("version") Integer version);
}