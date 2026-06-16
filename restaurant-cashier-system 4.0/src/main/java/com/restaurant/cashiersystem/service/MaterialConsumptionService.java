package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.cashiersystem.entity.MaterialConsumption;

public interface MaterialConsumptionService extends IService<MaterialConsumption> {
    void consumeOrderMaterials(Long orderId);
}