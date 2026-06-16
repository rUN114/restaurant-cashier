package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.cashiersystem.entity.Table;
import java.util.List;

public interface TableService extends IService<Table> {
    List<Table> getTablesByStore(Long storeId);
    List<Table> getFreeTables(Long storeId);
    void updateTableStatus(Long tableId, String status);
}