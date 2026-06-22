package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.MaterialInventory;
import com.restaurant.cashiersystem.entity.Purchase;
import com.restaurant.cashiersystem.mapper.PurchaseMapper;
import com.restaurant.cashiersystem.service.MaterialInventoryService;
import com.restaurant.cashiersystem.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class PurchaseServiceImpl extends ServiceImpl<PurchaseMapper, Purchase> implements PurchaseService {

    @Autowired
    private MaterialInventoryService materialInventoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Purchase purchase) {
        boolean save = super.save(purchase);

        // FOR UPDATE 行锁防止并发采购同一原料时丢失更新
        MaterialInventory inventory = materialInventoryService.lambdaQuery()
                .eq(MaterialInventory::getStoreId, purchase.getStoreId())
                .eq(MaterialInventory::getMaterialId, purchase.getMaterialId())
                .last("FOR UPDATE")
                .one();

        if (inventory == null) {
            inventory = new MaterialInventory();
            inventory.setStoreId(purchase.getStoreId());
            inventory.setMaterialId(purchase.getMaterialId());
            inventory.setCurrentStock(purchase.getQuantity());
            inventory.setCostPrice(purchase.getUnitPrice());
            return materialInventoryService.save(inventory);
        } else {
            inventory.setCurrentStock(inventory.getCurrentStock().add(purchase.getQuantity()));
            return materialInventoryService.updateById(inventory);
        }
    }
}