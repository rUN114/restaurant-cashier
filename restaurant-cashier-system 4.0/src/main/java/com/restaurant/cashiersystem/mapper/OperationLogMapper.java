package com.restaurant.cashiersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.cashiersystem.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
