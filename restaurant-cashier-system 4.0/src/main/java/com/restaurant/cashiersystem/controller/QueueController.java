package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.WaitingQueue;
import com.restaurant.cashiersystem.mapper.WaitingQueueMapper;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "等位排队", description = "取号、叫号、等位大屏")
@RestController
@RequestMapping("/api/queue")
public class QueueController {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private WaitingQueueMapper waitingQueueMapper;
    @Autowired private StringRedisTemplate stringRedisTemplate;

    /** 取号 — Redis 分布式锁防并发竞态 */
    @PostMapping("/take")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','waiter')")
    public Result<WaitingQueue> take(@RequestBody WaitingQueue q, HttpServletRequest req) {
        // 非全局管理员强制使用自己的门店
        Long forcedStoreId = forceStoreId(req);
        Long storeId = forcedStoreId != null ? forcedStoreId : q.getStoreId();
        q.setStoreId(storeId);

        // 生成排队号前缀
        String prefix = q.getTablePref() != null && q.getTablePref().contains("包厢") ? "V" :
                        q.getTablePref() != null && q.getTablePref().contains("中") ? "B" : "A";

        // Redis 分布式锁：同一门店+前缀每次只允许一个取号请求
        String lockKey = "queue:take:" + storeId + ":" + prefix;
        int retries = 3;
        while (retries-- > 0) {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", Duration.ofSeconds(2));
            if (Boolean.TRUE.equals(acquired)) {
                try {
                    int count = Math.toIntExact(new LambdaQueryWrapper<WaitingQueue>()
                            .eq(WaitingQueue::getStoreId, storeId)
                            .likeRight(WaitingQueue::getQueueNumber, prefix)
                            .count()) + 1;
                    q.setQueueNumber(prefix + String.format("%03d", count));
                    q.setStatus("waiting");
                    q.setCreatedAt(LocalDateTime.now());
                    waitingQueueMapper.insert(q);
                    return Result.success(q);
                } finally {
                    stringRedisTemplate.delete(lockKey);
                }
            }
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }
        return Result.error("取号繁忙，请稍后再试");
    }

    /** 当前等位列表 */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','waiter')")
    public Result<List<WaitingQueue>> list(@RequestParam(required = false) Long storeId,
                                           HttpServletRequest req) {
        storeId = resolveStoreId(storeId, req);
        var w = new LambdaQueryWrapper<WaitingQueue>();
        w.eq(WaitingQueue::getStatus, "waiting");
        if (storeId != null) w.eq(WaitingQueue::getStoreId, storeId);
        w.orderByAsc(WaitingQueue::getCreatedAt);
        return Result.success(waitingQueueMapper.selectList(w));
    }

    /** 叫号（安排入座） */
    @PutMapping("/call/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','waiter')")
    public Result<Void> call(@PathVariable Long id, @RequestParam(required = false) String table,
                             HttpServletRequest req) {
        WaitingQueue q = waitingQueueMapper.selectById(id);
        if (q == null) return Result.error("排队号不存在");
        if (!isAdmin(req) && !q.getStoreId().equals(getStoreId(req))) {
            return Result.error("无权操作其他门店的等位");
        }
        q.setStatus("seated");
        q.setSeatedTable(table);
        q.setSeatedAt(LocalDateTime.now());
        waitingQueueMapper.updateById(q);
        return Result.success();
    }

    /** 取消等位 */
    @PutMapping("/cancel/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','waiter')")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest req) {
        WaitingQueue q = waitingQueueMapper.selectById(id);
        if (q == null) return Result.error("排队号不存在");
        if (!isAdmin(req) && !q.getStoreId().equals(getStoreId(req))) {
            return Result.error("无权操作其他门店的等位");
        }
        q.setStatus("cancelled");
        waitingQueueMapper.updateById(q);
        return Result.success();
    }

    /** 最近叫号记录 */
    @GetMapping("/called")
    public Result<List<WaitingQueue>> called(@RequestParam(required = false) Long storeId,
                                              HttpServletRequest req) {
        storeId = resolveStoreId(storeId, req);
        var w = new LambdaQueryWrapper<WaitingQueue>();
        w.eq(WaitingQueue::getStatus, "seated");
        if (storeId != null) w.eq(WaitingQueue::getStoreId, storeId);
        w.orderByDesc(WaitingQueue::getSeatedAt).last("LIMIT 10");
        return Result.success(waitingQueueMapper.selectList(w));
    }

    /** 等位大屏数据：当前等待 + 桌台状态（参数化查询） */
    @GetMapping("/board")
    public Result<Map<String, Object>> board(@RequestParam(required = false) Long storeId,
                                              HttpServletRequest req) {
        storeId = resolveStoreId(storeId, req);
        var w = new LambdaQueryWrapper<WaitingQueue>();
        w.eq(WaitingQueue::getStatus, "waiting");
        if (storeId != null) w.eq(WaitingQueue::getStoreId, storeId);
        w.orderByAsc(WaitingQueue::getCreatedAt);
        List<WaitingQueue> waiting = waitingQueueMapper.selectList(w);

        List<Object> params = new ArrayList<>();
        String tsf = "";
        if (storeId != null) {
            tsf = " WHERE store_id = ?";
            params.add(storeId);
        }
        List<Map<String, Object>> tableRows = jdbcTemplate.queryForList(
            "SELECT COUNT(*) AS total, COALESCE(SUM(CASE WHEN status='occupied' THEN 1 ELSE 0 END),0) AS occ FROM tables" + tsf, params.toArray());
        Map<String, Object> tableRow = tableRows.isEmpty() ? Map.of("total", 0, "occ", 0) : tableRows.get(0);
        int total = ((Number) tableRow.get("total")).intValue();
        int occupied = ((Number) tableRow.get("occ")).intValue();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("waiting", waiting);
        res.put("totalTables", total);
        res.put("occupiedTables", occupied);
        res.put("freeTables", total - occupied);
        res.put("estimatedWait", waiting.size() * 5);
        return Result.success(res);
    }

    // ====== 内部方法 ======

    /** 非全局管理员强制使用自己的门店，防止跨店操作 */
    private Long forceStoreId(HttpServletRequest req) {
        return isAdmin(req) ? null : getStoreId(req);
    }

    /** 解析门店：非管理员只能查自己门店 */
    private Long resolveStoreId(Long paramStoreId, HttpServletRequest req) {
        if (isAdmin(req)) return paramStoreId;
        return getStoreId(req); // 忽略前端传参，强制用自己的门店
    }

    private boolean isAdmin(HttpServletRequest req) {
        return "admin_global".equals(getRole(req));
    }

    private String getRole(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            try { Claims c = jwtUtil.parseToken(h.substring(7)); return (String) c.get("roleCode"); }
            catch (Exception e) {}
        }
        return null;
    }

    private Long getStoreId(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            try { Claims c = jwtUtil.parseToken(h.substring(7)); return c.get("storeId", Long.class); }
            catch (Exception e) {}
        }
        return null;
    }
}
