package com.restaurant.cashiersystem.controller;

import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.Dish;
import com.restaurant.cashiersystem.mapper.DishMapper;
import com.restaurant.cashiersystem.service.SeckillService;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 秒杀控制器
 * <ul>
 *   <li>POST /api/seckill/{dishId} — 执行秒杀</li>
 *   <li>GET  /api/seckill/result — 查询秒杀结果（客户端超时后确认用）</li>
 *   <li>GET  /api/seckill/stock/{dishId} — 查询剩余库存</li>
 * </ul>
 */
@Tag(name = "秒杀管理", description = "一元秒杀菜品 — 高并发抢购")
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private static final Logger log = LoggerFactory.getLogger(SeckillController.class);

    /** 用户级限流 key 前缀，每人每秒最多 1 次秒杀请求 */
    private static final String USER_RATE_KEY = "seckill:rate:";
    private static final Duration RATE_WINDOW = Duration.ofSeconds(1);

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    @Qualifier("seckillExecutor")
    private Executor seckillExecutor;

    /**
     * 执行秒杀
     */
    @PostMapping("/{dishId}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'cashier', 'waiter')")
    @Operation(summary = "一元秒杀", description = "高并发秒杀接口。网络超时后调用 GET /api/seckill/result 查询结果")
    public Result<Map<String, Object>> seckill(
            @PathVariable Long dishId,
            @RequestParam String requestId,
            HttpServletRequest request) {

        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        // ---- 秒杀时间窗口校验 ----
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            return Result.error("菜品不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (dish.getSeckillStartTime() != null && now.isBefore(dish.getSeckillStartTime())) {
            return Result.error("秒杀尚未开始");
        }
        if (dish.getSeckillEndTime() != null && now.isAfter(dish.getSeckillEndTime())) {
            return Result.error("秒杀已结束");
        }

        // ---- 幂等：先查 Redis 结果缓存 ----
        String cached = seckillService.queryResult(requestId);
        if (!"UNKNOWN".equals(cached)) {
            return buildResult(cached, null, requestId);
        }

        // ---- 用户级限流：每人每秒 1 次 ----
        String rateKey = USER_RATE_KEY + userId;
        try {
            Boolean allowed = stringRedisTemplate.opsForValue()
                    .setIfAbsent(rateKey, "1", RATE_WINDOW);
            if (Boolean.FALSE.equals(allowed)) {
                return Result.error("请勿频繁操作，1 秒后再试");
            }
        } catch (Exception ignored) {
            // Redis 不可用时降级放行
        }

        // ---- 执行 Lua 脚本原子扣库存 ----
        int code = seckillService.trySeckill(dishId, userId, requestId);

        log.info("秒杀结果: dishId={}, userId={}, requestId={}, code={}", dishId, userId, requestId, code);

        switch (code) {
            case 1: {
                // 抢购成功 → 异步创建订单（使用专用线程池隔离秒杀流量）
                final Long uid = userId;
                CompletableFuture.runAsync(() -> {
                    try {
                        seckillService.createSeckillOrder(dishId, uid, requestId);
                    } catch (Exception e) {
                        log.error("异步创建秒杀订单失败: dishId={}, userId={}, requestId={}",
                                dishId, uid, requestId, e);
                        seckillService.rollbackRedis(dishId, uid);
                    }
                }, seckillExecutor);
                return buildResult("SUCCESS", null, requestId);
            }
            case 0:
                return buildResult("SOLD_OUT", "已被抢光，下次早点来", requestId);
            case -1:
                return buildResult("ALREADY_GRABBED", "您已抢过该菜品", requestId);
            case -3:
                // Redis 超时 → PENDING（不确定）
                return buildResult("PENDING",
                        "网络波动，请稍后查询结果。点击「我的订单」或重新调用结果接口", requestId);
            default:
                return Result.error("系统异常");
        }
    }

    /**
     * 查询秒杀结果 — 客户端超时/丢包后的唯一确认渠道
     */
    @GetMapping("/result")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'cashier', 'waiter')")
    @Operation(summary = "查询秒杀结果", description = "当 POST 请求超时/丢包时调用，确认到底抢没抢到")
    public Result<Map<String, Object>> queryResult(
            @RequestParam String requestId,
            @RequestParam Long dishId,
            HttpServletRequest request) {

        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        String status = seckillService.queryResult(requestId);
        return buildResult(status, null, requestId);
    }

    /**
     * 查询秒杀剩余库存（用于前端展示）
     */
    @GetMapping("/stock/{dishId}")
    @Operation(summary = "查询秒杀剩余库存")
    public Result<Map<String, Object>> getStock(@PathVariable Long dishId) {
        int stock = seckillService.getCurrentStock(dishId);
        Map<String, Object> data = Map.of("dishId", dishId, "remainingStock", stock);
        return Result.success(data);
    }

    // ==================== 内部方法 ====================

    private Result<Map<String, Object>> buildResult(String status, String customMsg, String requestId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", status);
        data.put("requestId", requestId);

        String message = switch (status) {
            case "SUCCESS" -> "恭喜，抢购成功！订单正在生成中…";
            case "SOLD_OUT" -> customMsg != null ? customMsg : "已被抢光";
            case "ALREADY_GRABBED" -> customMsg != null ? customMsg : "您已抢过该菜品";
            case "PENDING" -> customMsg != null ? customMsg : "结果确认中，请稍后查询";
            case "UNKNOWN" -> "暂无结果，请确认 requestId 是否正确或稍后重试";
            default -> "未知状态";
        };
        data.put("message", message);

        // PENDING 和 UNKNOWN 均非成功状态 — 前端据此展示「结果确认中」
        if ("PENDING".equals(status) || "UNKNOWN".equals(status)) {
            return Result.error(message);
        }
        return Result.success(data);
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
