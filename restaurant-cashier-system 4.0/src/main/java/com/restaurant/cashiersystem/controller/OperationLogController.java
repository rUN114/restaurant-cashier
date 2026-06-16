package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.common.PageResult;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.OperationLog;
import com.restaurant.cashiersystem.service.OperationLogService;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "操作日志", description = "操作日志查询与记录")
@RestController
@RequestMapping("/api/operation-logs")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 查询操作日志
     * 管理员：可传 storeId 过滤，不传则返回全部
     * 店长：只返回本店日志
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<List<OperationLog>> getLogs(@RequestParam(required = false) Long storeId,
                                              HttpServletRequest request) {
        // 从 token 中获取角色和门店ID
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.error("未登录");
        }
        String token = authHeader.substring(7);
        Claims claims = jwtUtil.parseToken(token);
        String roleCode = (String) claims.get("roleCode");
        Long userStoreId = claims.get("storeId", Long.class);

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if ("admin_global".equals(roleCode)) {
            // 管理员：如果传了 storeId 则过滤，否则不限制
            if (storeId != null) {
                wrapper.eq(OperationLog::getStoreId, storeId);
            }
        } else if ("admin_store".equals(roleCode)) {
            // 店长：强制只看本店
            if (userStoreId == null) {
                return Result.error("店长未绑定门店");
            }
            wrapper.eq(OperationLog::getStoreId, userStoreId);
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt);
        return Result.success(operationLogService.list(wrapper));
    }

    /**
     * 查询操作日志（分页版本）
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public PageResult<List<OperationLog>> getLogsPage(
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未登录");
        }
        String token = authHeader.substring(7);
        Claims claims = jwtUtil.parseToken(token);
        String roleCode = (String) claims.get("roleCode");
        Long userStoreId = claims.get("storeId", Long.class);

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if ("admin_global".equals(roleCode)) {
            if (storeId != null) {
                wrapper.eq(OperationLog::getStoreId, storeId);
            }
        } else if ("admin_store".equals(roleCode)) {
            wrapper.eq(OperationLog::getStoreId, userStoreId);
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OperationLog> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        operationLogService.page(page, wrapper);

        return PageResult.success(page.getRecords(), page.getTotal(), page.getPages(),
            (int) page.getCurrent(), (int) page.getSize());
    }

    @PostMapping
    public Result<Long> createLog(@RequestBody OperationLog log) {
        operationLogService.save(log);
        return Result.success(log.getId());
    }
}