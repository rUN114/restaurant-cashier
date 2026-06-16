package com.restaurant.cashiersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.cashiersystem.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}

