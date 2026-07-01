package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.Purchase;
import com.restaurant.cashiersystem.mapper.PurchaseMapper;
import com.restaurant.cashiersystem.service.PurchaseService;
import org.springframework.stereotype.Service;

@Service
public class PurchaseServiceImpl extends ServiceImpl<PurchaseMapper, Purchase> implements PurchaseService {

    @Override
    public boolean save(Purchase purchase) {
        return super.save(purchase);
    }
}
