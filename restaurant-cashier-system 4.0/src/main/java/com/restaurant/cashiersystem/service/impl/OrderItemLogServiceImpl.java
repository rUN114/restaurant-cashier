package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.OrderItemLog;
import com.restaurant.cashiersystem.mapper.OrderItemLogMapper;
import com.restaurant.cashiersystem.service.OrderItemLogService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜品流水日志实现
 */
@Service
public class OrderItemLogServiceImpl extends ServiceImpl<OrderItemLogMapper, OrderItemLog> implements OrderItemLogService {

    @Override
    public List<OrderItemLog> getLogsByOrderId(Long orderId) {
        LambdaQueryWrapper<OrderItemLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItemLog::getOrderId, orderId);
        return list(wrapper);
    }
}