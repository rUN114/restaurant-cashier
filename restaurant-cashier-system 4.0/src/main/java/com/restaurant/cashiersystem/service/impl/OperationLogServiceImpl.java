package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.OperationLog;
import com.restaurant.cashiersystem.mapper.OperationLogMapper;
import com.restaurant.cashiersystem.service.OperationLogService;
import org.springframework.stereotype.Service;

/**
 * 操作日志实现
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

}