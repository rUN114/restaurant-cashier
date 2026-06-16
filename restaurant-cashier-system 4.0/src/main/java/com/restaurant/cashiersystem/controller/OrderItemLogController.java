package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.OrderItemLog;
import com.restaurant.cashiersystem.service.OrderItemLogService;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Tag(name = "菜品操作流水", description = "加菜、退菜等操作流水查询与记录")
@RestController
@RequestMapping("/api/order-item-logs")
public class OrderItemLogController {

    private static final Logger log = LoggerFactory.getLogger(OrderItemLogController.class);

    @Autowired
    private OrderItemLogService orderItemLogService;

    @Autowired
    private JwtUtil jwtUtil;   // 用于解析当前用户身份

    /**
     * 查询所有菜品操作流水（管理员看全部，店长看本店）
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<List<OrderItemLog>> getAllLogs(HttpServletRequest request) {
        // 从 token 中获取角色和门店 ID
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.error("未登录");
        }
        String token = authHeader.substring(7);
        Claims claims = jwtUtil.parseToken(token);
        String roleCode = (String) claims.get("roleCode");
        Long storeId = claims.get("storeId", Long.class);

        LambdaQueryWrapper<OrderItemLog> wrapper = new LambdaQueryWrapper<>();
        if ("admin_store".equals(roleCode)) {
            if (storeId == null) {
                return Result.error("店长未绑定门店");
            }
            wrapper.eq(OrderItemLog::getStoreId, storeId);
        }
        // 按操作时间倒序
        wrapper.orderByDesc(OrderItemLog::getCreatedAt);
        return Result.success(orderItemLogService.list(wrapper));
    }

    /**
     * 根据订单ID查询菜品操作流水（保留原接口）
     */
    @GetMapping("/order/{orderId}")
    public Result<List<OrderItemLog>> getLogsByOrderId(@PathVariable Long orderId) {
        List<OrderItemLog> list = orderItemLogService.getLogsByOrderId(orderId);
        return Result.success(list);
    }

    @PostMapping
    public Result<Long> createLog(@RequestBody OrderItemLog log) {
        orderItemLogService.save(log);
        return Result.success(log.getId());
    }
}