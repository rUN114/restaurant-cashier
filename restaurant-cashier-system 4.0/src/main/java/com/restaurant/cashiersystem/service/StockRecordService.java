package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.cashiersystem.entity.StockRecord;
import java.util.List;

public interface StockRecordService extends IService<StockRecord> {
    List<StockRecord> getByStoreId(Long storeId);
}
