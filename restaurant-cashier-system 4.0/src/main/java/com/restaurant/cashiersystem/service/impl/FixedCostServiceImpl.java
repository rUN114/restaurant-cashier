package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.FixedCost;
import com.restaurant.cashiersystem.mapper.FixedCostMapper;
import com.restaurant.cashiersystem.service.FixedCostService;
import org.springframework.stereotype.Service;

@Service
public class FixedCostServiceImpl extends ServiceImpl<FixedCostMapper, FixedCost> implements FixedCostService {
}