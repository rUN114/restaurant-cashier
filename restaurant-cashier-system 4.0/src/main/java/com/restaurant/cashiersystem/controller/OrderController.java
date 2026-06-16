package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.common.PageResult;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.Order;
import com.restaurant.cashiersystem.entity.Table;
import com.restaurant.cashiersystem.entity.OperationLog;
import com.restaurant.cashiersystem.service.OrderService;
import com.restaurant.cashiersystem.service.TableService;
import com.restaurant.cashiersystem.service.OperationLogService;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Tag(name = "订单管理", description = "开台、结算、查询订单及营业额")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private TableService tableService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OperationLogService operationLogService;


    @GetMapping("/store/{storeId}")
    public Result<List<Order>> getOrdersByStore(@PathVariable Long storeId) {
        List<Order> list = orderService.getOrdersByStore(storeId);
        return Result.success(list);
    }

    @GetMapping("/progress/{storeId}")
    public Result<List<Order>> getInProgressOrders(@PathVariable Long storeId) {
        List<Order> list = orderService.getInProgressOrders(storeId);
        return Result.success(list);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'waiter')")
    public Result<Long> createOrder(@RequestBody Order order,
                                    HttpServletRequest request) {
        // ---- 桌台校验：同一桌台已有进行中订单，禁止重复开单 ----
        if (order.getTableId() != null) {
            LambdaQueryWrapper<Order> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(Order::getTableId, order.getTableId())
                        .eq(Order::getStatus, "in_progress");
            long count = orderService.count(checkWrapper);
            if (count > 0) {
                return Result.error("该桌尚有未完成订单，无法重复开单");
            }
        }

        // 生成订单号
        String orderNo = "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + String.format("%03d", new Random().nextInt(1000));
        order.setOrderNo(orderNo);

        Table table = null;
        // 设置桌号（根据桌台ID查询）
        if (order.getTableId() != null) {
            table = tableService.getById(order.getTableId());
            if (table != null) {
                order.setTableNumber(table.getTableNumber());
            }
        }

        // 设置创建者（当前用户ID）
        Long userId = getUserIdFromToken(request);
        if (userId != null) {
            order.setCreatedBy(userId);
        }

        orderService.save(order);

        // 开单后自动将桌台设为占用（防止前后端状态不一致）
        if (table != null) {
            table.setStatus("occupied");
            tableService.updateById(table);
        }

        // 新增操作日志记录
        OperationLog opLog = new OperationLog();
        opLog.setStoreId(order.getStoreId());
        Long operatorId = getUserIdFromToken(request);
        opLog.setOperatorId(operatorId);
        opLog.setTargetType("order");
        opLog.setTargetId(order.getId().toString());
        opLog.setAction("openTable");
        opLog.setDetail("开单，桌号：" + (table != null ? table.getTableNumber() : "无"));
        opLog.setCreatedAt(LocalDateTime.now());
        operationLogService.save(opLog);

        return Result.success(order.getId());
    }

    @PutMapping("/settle/{orderId}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'cashier')")
    public Result<Void> settleOrder(
            @PathVariable Long orderId,
            @RequestParam Long cashierId,
            @RequestParam String paymentMethod,
            @RequestParam(required = false, defaultValue = "0") BigDecimal discountAmount,
            @RequestParam(required = false, defaultValue = "0") BigDecimal finalAmount,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false, defaultValue = "false") boolean useMemberDiscount,
            @RequestParam(required = false, defaultValue = "0") int redeemPoints) {
        orderService.settleOrder(orderId, cashierId, paymentMethod,
                discountAmount, finalAmount, memberId, useMemberDiscount, redeemPoints);
        return Result.success();
    }

    @PutMapping("/cancel/{orderId}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'waiter')")
    public Result<Void> cancelOrder(@PathVariable Long orderId,
                                    HttpServletRequest request) {
        Long operatorId = getUserIdFromToken(request);
        if (operatorId == null) {
            return Result.error("无法获取操作员信息");
        }
        orderService.cancelOrder(orderId, operatorId);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Order> getOrderById(@PathVariable Long id) {
        Order order = orderService.getById(id);
        return Result.success(order);
    }

    @GetMapping("/turnover/{storeId}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<BigDecimal> getStoreTurnover(@PathVariable Long storeId) {
        BigDecimal total = orderService.getStoreTurnover(storeId);
        return Result.success(total);
    }

    @GetMapping("/today-turnover/{storeId}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<BigDecimal> getTodayTurnover(@PathVariable Long storeId) {
        return Result.success(orderService.getTodayTurnover(storeId));
    }

    @GetMapping("/month-turnover/{storeId}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<BigDecimal> getMonthTurnover(@PathVariable Long storeId) {
        return Result.success(orderService.getMonthTurnover(storeId));
    }

    /**
     * 分页查询订单
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','cashier','waiter','chef')")
    public PageResult<List<Order>> getOrdersPage(
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (storeId != null) {
            wrapper.eq(Order::getStoreId, storeId);
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        orderService.page(page, wrapper);

        return PageResult.success(page.getRecords(), page.getTotal(), page.getPages(),
            (int) page.getCurrent(), (int) page.getSize());
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('admin_global')")
    public Result<List<Order>> getAllOrders() {
        List<Order> list = orderService.list();
        return Result.success(list);
    }

    private Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtUtil.parseToken(token);
                String subject = claims.getSubject();
                return Long.valueOf(subject);
            } catch (Exception e) {
                log.error("解析token失败", e);
            }
        }
        return null;
    }
}