package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.OperationLog;
import com.restaurant.cashiersystem.mapper.OperationLogMapper;
import com.restaurant.cashiersystem.service.OperationLogService;
import com.restaurant.cashiersystem.util.RequestContext;
import org.springframework.stereotype.Service;

/**
 * 操作日志实现：save 时自动填入请求 IP
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public boolean save(OperationLog entity) {
        if (entity.getOperatorIp() == null) {
            entity.setOperatorIp(RequestContext.getClientIp());
        }
        return super.save(entity);
    }
}