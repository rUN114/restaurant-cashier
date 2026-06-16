package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.Store;
import com.restaurant.cashiersystem.mapper.StoreMapper;
import com.restaurant.cashiersystem.service.StoreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分店业务实现
 */
@Service
public class StoreServiceImpl extends ServiceImpl<StoreMapper, Store> implements StoreService {

    @Override
    public List<Store> getValidStores() {
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Store::getDeletedAt);
        wrapper.eq(Store::getStatus, 1);
        return list(wrapper);
    }

    @Override
    public Store getStoreById(Long storeId) {
        return getById(storeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteStoreLogic(Long storeId) {
        Store store = getById(storeId);
        if (store == null) return false;
        store.setDeletedAt(LocalDateTime.now());
        return updateById(store);
    }
}