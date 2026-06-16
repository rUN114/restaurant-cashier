package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.cashiersystem.entity.Store;
import java.util.List;

/**
 * 分店业务接口
 */
public interface StoreService extends IService<Store> {

    /**
     * 获取所有有效分店（未删除、营业中）
     */
    List<Store> getValidStores();

    /**
     * 根据ID查询分店
     */
    Store getStoreById(Long storeId);

    /**
     * 软删除分店
     */
    boolean deleteStoreLogic(Long storeId);
}