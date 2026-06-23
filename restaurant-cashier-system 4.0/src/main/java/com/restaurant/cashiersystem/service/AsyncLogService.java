package com.restaurant.cashiersystem.service;

import com.restaurant.cashiersystem.entity.OperationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步日志服务：操作日志、登录日志等不阻塞主业务流程的写操作
 */
@Service
public class AsyncLogService {

    private static final Logger log = LoggerFactory.getLogger(AsyncLogService.class);

    @Autowired
    private OperationLogService operationLogService;

    @Async
    public void saveOperationLog(OperationLog opLog) {
        try {
            operationLogService.save(opLog);
        } catch (Exception e) {
            log.warn("异步写操作日志失败", e);
        }
    }
}
