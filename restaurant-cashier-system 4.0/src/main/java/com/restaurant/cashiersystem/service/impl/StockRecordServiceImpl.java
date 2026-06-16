package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.StockRecord;
import com.restaurant.cashiersystem.mapper.StockRecordMapper;
import com.restaurant.cashiersystem.service.StockRecordService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class StockRecordServiceImpl extends ServiceImpl<StockRecordMapper, StockRecord> implements StockRecordService {

    @Override
    public List<StockRecord> getByStoreId(Long storeId) {
        LambdaQueryWrapper<StockRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockRecord::getStoreId, storeId);
        wrapper.orderByDesc(StockRecord::getCreatedAt);
        return list(wrapper);
    }
}
