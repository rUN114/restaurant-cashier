package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.WaitingQueue;
import com.restaurant.cashiersystem.mapper.WaitingQueueMapper;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "等位排队", description = "取号、叫号、等位大屏")
@RestController
@RequestMapping("/api/queue")
public class QueueController extends ServiceImpl<WaitingQueueMapper, WaitingQueue> {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtUtil jwtUtil;

    /** 取号 */
    @PostMapping("/take")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','waiter')")
    public Result<WaitingQueue> take(@RequestBody WaitingQueue q, HttpServletRequest req) {
        if (q.getStoreId() == null) q.setStoreId(getStoreId(req));
        // 生成排队号
        String prefix = q.getTablePref() != null && q.getTablePref().contains("包厢") ? "V" :
                        q.getTablePref() != null && q.getTablePref().contains("中") ? "B" : "A";
        int count = Math.toIntExact(lambdaQuery()
                .eq(WaitingQueue::getStoreId, q.getStoreId())
                .likeRight(WaitingQueue::getQueueNumber, prefix)
                .count()) + 1;
        q.setQueueNumber(prefix + String.format("%03d", count));
        q.setStatus("waiting");
        q.setCreatedAt(LocalDateTime.now());
        save(q);
        return Result.success(q);
    }

    /** 当前等位列表 */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','waiter')")
    public Result<List<WaitingQueue>> list(@RequestParam(required = false) Long storeId) {
        var w = new LambdaQueryWrapper<WaitingQueue>();
        w.eq(WaitingQueue::getStatus, "waiting");
        if (storeId != null) w.eq(WaitingQueue::getStoreId, storeId);
        w.orderByAsc(WaitingQueue::getCreatedAt);
        return Result.success(list(w));
    }

    /** 叫号（安排入座） */
    @PutMapping("/call/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','waiter')")
    public Result<Void> call(@PathVariable Long id, @RequestParam(required = false) String table) {
        WaitingQueue q = getById(id);
        if (q == null) return Result.error("排队号不存在");
        q.setStatus("seated");
        q.setSeatedTable(table);
        q.setSeatedAt(LocalDateTime.now());
        updateById(q);
        return Result.success();
    }

    /** 取消等位 */
    @PutMapping("/cancel/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','waiter')")
    public Result<Void> cancel(@PathVariable Long id) {
        WaitingQueue q = getById(id);
        if (q == null) return Result.error("排队号不存在");
        q.setStatus("cancelled");
        updateById(q);
        return Result.success();
    }

    /** 最近叫号记录 */
    @GetMapping("/called")
    public Result<List<WaitingQueue>> called(@RequestParam(required = false) Long storeId) {
        var w = new LambdaQueryWrapper<WaitingQueue>();
        w.eq(WaitingQueue::getStatus, "seated");
        if (storeId != null) w.eq(WaitingQueue::getStoreId, storeId);
        w.orderByDesc(WaitingQueue::getSeatedAt).last("LIMIT 10");
        return Result.success(list(w));
    }

    /** 等位大屏数据：当前等待 + 桌台状态 */
    @GetMapping("/board")
    public Result<Map<String, Object>> board(@RequestParam(required = false) Long storeId) {
        var w = new LambdaQueryWrapper<WaitingQueue>();
        w.eq(WaitingQueue::getStatus, "waiting");
        if (storeId != null) w.eq(WaitingQueue::getStoreId, storeId);
        w.orderByAsc(WaitingQueue::getCreatedAt);
        List<WaitingQueue> waiting = list(w);

        String tsf = storeId != null ? " WHERE store_id = " + storeId : "";
        var tableRow = jdbcTemplate.queryForMap(
            "SELECT COUNT(*) AS total, COALESCE(SUM(CASE WHEN status='occupied' THEN 1 ELSE 0 END),0) AS occ FROM tables" + tsf);
        int total = ((Number) tableRow.get("total")).intValue();
        int occupied = ((Number) tableRow.get("occ")).intValue();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("waiting", waiting);
        res.put("totalTables", total);
        res.put("occupiedTables", occupied);
        res.put("freeTables", total - occupied);
        res.put("estimatedWait", waiting.size() * 5); // 每桌约5分钟
        return Result.success(res);
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
