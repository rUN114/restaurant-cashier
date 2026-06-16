package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.MaterialInventory;
import com.restaurant.cashiersystem.mapper.MaterialInventoryMapper;
import com.restaurant.cashiersystem.service.MaterialInventoryService;
import org.springframework.stereotype.Service;

@Service
public class MaterialInventoryServiceImpl extends ServiceImpl<MaterialInventoryMapper, MaterialInventory> implements MaterialInventoryService {
}