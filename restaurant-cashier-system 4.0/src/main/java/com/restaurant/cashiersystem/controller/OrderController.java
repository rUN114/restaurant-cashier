package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.common.PageResult;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.Order;
import com.restaurant.cashiersystem.entity.Table;
import com.restaurant.cashiersystem.entity.OperationLog;
import com.restaurant.cashiersystem.service.OrderService;
import com.restaurant.cashiersystem.service.AsyncLogService;
import com.restaurant.cashiersystem.service.StoreService;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    @Autowired
    private StoreService storeService;

    @Autowired
    private AsyncLogService asyncLogService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 订单幂等 key 前缀，TTL 5 分钟防网络重放 */
    private static final String IDEMPOTENT_ORDER_PREFIX = "idempotent:order:";
    private static final Duration IDEMPOTENT_TTL = Duration.ofMinutes(5);

    /** 开单桌台锁 key 前缀，同一桌台同时只能开一单 */
    private static final String TABLE_LOCK_PREFIX = "table:lock:";
    private static final Duration TABLE_LOCK_TTL = Duration.ofSeconds(3);


    @GetMapping("/store/{storeId}")
    public Result<List<Order>> getOrdersByStore(@PathVariable Long storeId,
                                                HttpServletRequest request) {
        if (!canAccessStore(request, storeId)) return Result.error("无权查看该门店数据");
        List<Order> list = orderService.getOrdersByStore(storeId);
        return Result.success(list);
    }

    @GetMapping("/progress/{storeId}")
    public Result<List<Order>> getInProgressOrders(@PathVariable Long storeId,
                                                    HttpServletRequest request) {
        if (!canAccessStore(request, storeId)) return Result.error("无权查看该门店数据");
        List<Order> list = orderService.getInProgressOrders(storeId);
        return Result.success(list);
    }

    @Transactional(rollbackFor = Exception.class)
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'waiter')")
    public Result<Long> createOrder(@RequestBody Order order,
                                    HttpServletRequest request) {
        // ---- 店铺状态校验：停业店铺禁止开单 ----
        storeService.checkStoreActive(order.getStoreId());

        // ---- 桌台校验：Redis 分布式锁 + DB 双重防并发开单 ----
        String lockKey = null;
        boolean lockAcquired = false;
        if (order.getTableId() != null) {
            lockKey = TABLE_LOCK_PREFIX + order.getTableId();
            try {
                Boolean locked = stringRedisTemplate.opsForValue()
                        .setIfAbsent(lockKey, "1", TABLE_LOCK_TTL);
                if (Boolean.FALSE.equals(locked)) {
                    return Result.error("该桌正在开单中，请稍后再试");
                }
                lockAcquired = true;
            } catch (Exception e) {
                log.warn("Redis 不可用，跳过开单锁: tableId={}", order.getTableId(), e);
            }
            LambdaQueryWrapper<Order> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(Order::getTableId, order.getTableId())
                        .eq(Order::getStatus, "in_progress");
            long count = orderService.count(checkWrapper);
            if (count > 0) {
                return Result.error("该桌尚有未完成订单，无法重复开单");
            }
        }

        // 生成订单号：时间戳(17位) + 安全随机(5位) → 碰撞概率 ≈ 1/100000
        String orderNo = "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + String.format("%05d", new SecureRandom().nextInt(100000));
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

        // 开单后自动将桌台设为占用
        if (table != null) {
            table.setStatus("occupied");
            tableService.updateById(table);
        }

        // 新增操作日志记录
        OperationLog opLog = new OperationLog();
        opLog.setStoreId(order.getStoreId());
        opLog.setOperatorId(userId);
        opLog.setTargetType("order");
        opLog.setTargetId(order.getId().toString());
        opLog.setAction("openTable");
        opLog.setDetail("开单，桌号：" + (table != null ? table.getTableNumber() : "无"));
        opLog.setCreatedAt(LocalDateTime.now());
        asyncLogService.saveOperationLog(opLog);

        // 开单完成后释放桌台锁（防 TOCTOU 竞态）
        if (lockAcquired) {
            try { stringRedisTemplate.delete(lockKey); } catch (Exception ignored) {}
        }

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
            @RequestParam(required = false, defaultValue = "0") int redeemPoints,
            @RequestParam(required = false) String requestId) {
        // ===== 幂等校验：防止网络重放重复结账 =====
        if (requestId != null && !requestId.isBlank()) {
            try {
                String idempotentKey = IDEMPOTENT_ORDER_PREFIX + requestId;
                Boolean isFirst = stringRedisTemplate.opsForValue()
                        .setIfAbsent(idempotentKey, orderId.toString(), IDEMPOTENT_TTL);
                if (Boolean.FALSE.equals(isFirst)) {
                    log.warn("重复的结账请求已被拦截: orderId={}, requestId={}", orderId, requestId);
                    return Result.success();
                }
            } catch (Exception e) {
                log.warn("Redis 不可用，跳过幂等校验: orderId={}", orderId, e);
            }
        }
        // ===== 幂等校验结束 =====

        // 店铺状态校验
        Order order = orderService.getById(orderId);
        if (order != null) storeService.checkStoreActive(order.getStoreId());

        try {
            orderService.settleOrder(orderId, cashierId, paymentMethod,
                    discountAmount, finalAmount, memberId, useMemberDiscount, redeemPoints);
            return Result.success();
        } catch (Exception e) {
            // 结算失败 → 删除幂等 key，允许用户重试
            if (requestId != null && !requestId.isBlank()) {
                try {
                    stringRedisTemplate.delete(IDEMPOTENT_ORDER_PREFIX + requestId);
                    log.info("结算失败，已删除幂等 key: requestId={}", requestId);
                } catch (Exception ignored) {}
            }
            throw e;
        }
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
        // 通知 KDS 厨师：订单已取消
        messagingTemplate.convertAndSend("/topic/kitchen",
                (Object) java.util.Map.of("event", "order_cancelled", "data", java.util.Map.of("orderId", orderId)));
        messagingTemplate.convertAndSend("/topic/waiter",
                (Object) java.util.Map.of("event", "order_cancelled", "data", java.util.Map.of("orderId", orderId)));
        return Result.success();
    }

    @PutMapping("/reverse-settle/{orderId}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Void> reverseSettle(@PathVariable Long orderId,
                                       HttpServletRequest request) {
        Long operatorId = getUserIdFromToken(request);
        if (operatorId == null) return Result.error("无法获取操作员信息");
        try {
            orderService.reverseSettlement(orderId, operatorId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<Order> getOrderById(@PathVariable Long id,
                                      HttpServletRequest request) {
        Order order = orderService.getById(id);
        if (order != null && !canAccessStore(request, order.getStoreId())) {
            return Result.error("无权查看该订单");
        }
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
     * 交接班汇总：今日该收银员结算的订单数/金额 + 按支付方式分组
     */
    @GetMapping("/shift-summary")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','cashier')")
    public Result<Map<String, Object>> shiftSummary(HttpServletRequest request) {
        Long cashierId = getUserIdFromToken(request);
        Long storeId = getStoreIdFromToken(request);
        if ("admin_global".equals(getRoleFromToken(request))) storeId = null;

        // 今日该收银员结算的订单（参数化查询防SQL注入）
        List<Object> baseParams = new ArrayList<>();
        baseParams.add(cashierId);
        String baseSql = "SELECT o.payment_method, COUNT(*) AS cnt, COALESCE(SUM(o.final_amount),0) AS amt " +
                "FROM orders o WHERE o.status = 'settled' AND o.settled_by = ? " +
                "AND DATE(o.settled_at) = CURDATE()";
        if (storeId != null) {
            baseSql += " AND o.store_id = ?";
            baseParams.add(storeId);
        }
        baseSql += " GROUP BY o.payment_method";

        List<Map<String, Object>> breakdown = jdbcTemplate.query(baseSql,
                (rs, rowNum) -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("method", rs.getString("payment_method"));
                    m.put("count", rs.getInt("cnt"));
                    m.put("amount", rs.getBigDecimal("amt"));
                    return m;
                }, baseParams.toArray());

        // 计算合计
        int totalOrders = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map<String, Object> item : breakdown) {
            totalOrders += ((Number) item.get("count")).intValue();
            totalAmount = totalAmount.add((BigDecimal) item.get("amount"));
        }

        // 系统今日总营业额（该门店所有收银员合计，用于对账）
        List<Object> sysParams = new ArrayList<>();
        String sysSql = "SELECT COALESCE(SUM(o.final_amount),0) AS amt FROM orders o " +
                "WHERE o.status = 'settled' AND DATE(o.settled_at) = CURDATE()";
        if (storeId != null) {
            sysSql += " AND o.store_id = ?";
            sysParams.add(storeId);
        }
        BigDecimal systemTotal = jdbcTemplate.queryForObject(sysSql, BigDecimal.class,
                sysParams.isEmpty() ? null : sysParams.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cashierId", cashierId);
        result.put("storeId", storeId);
        result.put("totalOrders", totalOrders);
        result.put("totalAmount", totalAmount);
        result.put("systemTotal", systemTotal != null ? systemTotal : BigDecimal.ZERO);
        result.put("breakdown", breakdown);
        return Result.success(result);
    }

    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    @Autowired private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

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

    /** 判断当前用户是否有权访问指定门店数据 */
    private boolean canAccessStore(HttpServletRequest request, Long targetStoreId) {
        if (targetStoreId == null) return true;
        String roleCode = getRoleFromToken(request);
        if ("admin_global".equals(roleCode)) return true;
        Long userStoreId = getStoreIdFromToken(request);
        return targetStoreId.equals(userStoreId);
    }

    private String getRoleFromToken(HttpServletRequest request) {
        return extractClaim(request, "roleCode", String.class);
    }

    private Long getStoreIdFromToken(HttpServletRequest request) {
        return extractClaim(request, "storeId", Long.class);
    }

    @SuppressWarnings("unchecked")
    private <T> T extractClaim(HttpServletRequest request, String key, Class<T> type) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parseToken(authHeader.substring(7));
                Object value = claims.get(key);
                if (type == Long.class && value instanceof Integer) {
                    return (T) Long.valueOf((Integer) value);
                }
                return (T) value;
            } catch (Exception e) {
                log.debug("提取 JWT claim 失败: key={}", key);
            }
        }
        return null;
    }
}