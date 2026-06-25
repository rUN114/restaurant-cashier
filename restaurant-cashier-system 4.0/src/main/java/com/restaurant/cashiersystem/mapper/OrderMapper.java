package com.restaurant.cashiersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.cashiersystem.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /** 行级锁查询订单，防并发重复结算 */
    @Select("SELECT * FROM orders WHERE id = #{id} AND deleted_at IS NULL FOR UPDATE")
    Order selectByIdForUpdate(Long id);
}
