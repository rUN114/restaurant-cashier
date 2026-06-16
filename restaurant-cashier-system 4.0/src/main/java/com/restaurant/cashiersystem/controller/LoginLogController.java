package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.common.PageResult;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.LoginLog;
import com.restaurant.cashiersystem.service.LoginLogService;
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

@Tag(name = "登录日志", description = "登录日志查询与记录")
@RestController
@RequestMapping("/api/login-logs")
public class LoginLogController {

    private static final Logger log = LoggerFactory.getLogger(LoginLogController.class);

    @Autowired
    private LoginLogService loginLogService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 查询登录日志（管理员看全部，店长看本店）
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<List<LoginLog>> getLoginLogs(HttpServletRequest request) {
        // 从请求头中获取 token 并解析角色
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.error("未登录");
        }
        String token = authHeader.substring(7);
        Claims claims = jwtUtil.parseToken(token);
        String roleCode = (String) claims.get("roleCode"); // JWT 中保存的是原始角色码（admin_global等）
        Long storeId = claims.get("storeId", Long.class);  // 可能为 null

        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();

        // 店长只能看本店日志
        if ("admin_store".equals(roleCode)) {
            if (storeId == null) {
                return Result.error("店长未绑定门店");
            }
            wrapper.eq(LoginLog::getStoreId, storeId);
        }
        // 管理员则不过滤，查看全部（wrapper 为空）

        wrapper.orderByDesc(LoginLog::getCreatedAt);
        List<LoginLog> list = loginLogService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 查询登录日志（分页版本）
     * 管理员看全部，店长看本店
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public PageResult<List<LoginLog>> getLoginLogsPage(
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
        Long storeId = claims.get("storeId", Long.class);

        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        if ("admin_store".equals(roleCode)) {
            wrapper.eq(LoginLog::getStoreId, storeId);
        }
        wrapper.orderByDesc(LoginLog::getCreatedAt);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<LoginLog> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        loginLogService.page(page, wrapper);

        return PageResult.success(page.getRecords(), page.getTotal(), page.getPages(),
            (int) page.getCurrent(), (int) page.getSize());
    }

    /**
     * 新增登录日志（通常由登录逻辑自动记录，这里保留接口）
     */
    @PostMapping
    public Result<Long> createLoginLog(@RequestBody LoginLog log) {
        loginLogService.save(log);
        return Result.success(log.getId());
    }
}