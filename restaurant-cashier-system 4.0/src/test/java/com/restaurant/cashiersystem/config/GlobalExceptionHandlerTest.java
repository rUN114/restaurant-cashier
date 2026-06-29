package com.restaurant.cashiersystem.config;

import com.restaurant.cashiersystem.common.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.MyBatisSystemException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("全局异常处理")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("乐观锁冲突时返回友好提示")
    void optimisticLockReturnsUserFriendlyMessage() {
        // 模拟 MyBatis Plus 乐观锁异常
        RuntimeException optimisticEx = new RuntimeException("OptimisticLockException: version mismatch");
        MyBatisSystemException mybatisEx = new MyBatisSystemException(optimisticEx);

        Result<Void> result = handler.handleOptimisticLock(mybatisEx);

        assertNotNull(result);
        assertNotNull(result);
        assertTrue(result.getMsg().contains("已被他人修改"));
    }

    @Test
    @DisplayName("普通 MyBatis 异常返回通用错误")
    void regularMyBatisErrorReturnsGenericMessage() {
        RuntimeException cause = new RuntimeException("Table 'xxx' doesn't exist");
        MyBatisSystemException mybatisEx = new MyBatisSystemException(cause);

        Result<Void> result = handler.handleOptimisticLock(mybatisEx);

        assertNotNull(result);
        assertTrue(result.getMsg().contains("系统异常"));
    }

    @Test
    @DisplayName("业务异常返回原始消息")
    void runtimeExceptionReturnsOriginalMessage() {
        Result<Void> result = handler.handleRuntimeException(
                new RuntimeException("库存不足"));

        assertNotNull(result);
        assertTrue(result.getMsg().contains("库存不足"));
    }

    @Test
    @DisplayName("空指针异常不泄露堆栈")
    void npeReturnsSafeMessage() {
        Result<Void> result = handler.handleNPE(new NullPointerException());

        assertNotNull(result);
        assertTrue(result.getMsg().contains("服务器内部错误"));
    }
}
