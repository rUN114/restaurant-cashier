package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.Table;
import com.restaurant.cashiersystem.mapper.TableMapper;
import com.restaurant.cashiersystem.service.TableService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TableServiceImpl extends ServiceImpl<TableMapper, Table> implements TableService {

    @Override
    public List<Table> getTablesByStore(Long storeId) {
        LambdaQueryWrapper<Table> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Table::getStoreId, storeId);
        wrapper.isNull(Table::getDeletedAt);
        return list(wrapper);
    }

    @Override
    public List<Table> getFreeTables(Long storeId) {
        LambdaQueryWrapper<Table> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Table::getStoreId, storeId);
        wrapper.eq(Table::getStatus, "free");
        wrapper.isNull(Table::getDeletedAt);
        return list(wrapper);
    }

    @Override
    @Transactional
    public void updateTableStatus(Long tableId, String status) {
        Table table = getById(tableId);
        if (table == null) return;
        table.setStatus(status);
        updateById(table);
    }
}