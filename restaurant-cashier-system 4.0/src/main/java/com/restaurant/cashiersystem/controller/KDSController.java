package com.restaurant.cashiersystem.controller;

import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.dto.KitchenItemVO;
import com.restaurant.cashiersystem.entity.OrderItem;
import com.restaurant.cashiersystem.service.OrderItemService;
import com.restaurant.cashiersystem.service.OrderService;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "后厨看板 KDS", description = "厨师看板 + 服务员操控台")
@RestController
@RequestMapping("/api/kds")
public class KDSController {

    @Autowired private OrderItemService orderItemService;
    @Autowired private OrderService orderService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private SimpMessagingTemplate messaging;

    // ========== 厨师端 ==========

    /** 厨师看板：待制作 + 制作中（已完成的不显示） */
    @GetMapping("/kitchen")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','chef')")
    public Result<Map<String, List<KitchenItemVO>>> kitchenBoard(HttpServletRequest request) {
        Long storeId = getStoreIdFromToken(request);
        List<OrderItem> items = orderItemService.getInProgressItemsByStore(storeId);
        // 批量加载订单表号，消除 N+1 查询
        Map<Long, String> orderTableMap = buildOrderTableMap(items);
        List<KitchenItemVO> vos = items.stream()
                .filter(i -> !"SERVED".equals(i.getKitchenStatus()))
                .map(i -> toVO(i, orderTableMap))
                .sorted((a, b) -> {
                    if (a.getBumpCount() > 0 && b.getBumpCount() == 0) return -1;
                    if (b.getBumpCount() > 0 && a.getBumpCount() == 0) return 1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());

        Map<String, List<KitchenItemVO>> result = new LinkedHashMap<>();
        result.put("WAITING", vos.stream().filter(v -> "WAITING".equals(v.getKitchenStatus())).collect(Collectors.toList()));
        result.put("COOKING", vos.stream().filter(v -> "COOKING".equals(v.getKitchenStatus())).collect(Collectors.toList()));
        result.put("DONE", vos.stream().filter(v -> "DONE".equals(v.getKitchenStatus())).collect(Collectors.toList()));
        return Result.success(result);
    }

    /** 厨师：开始制作 */
    @PostMapping("/start/{itemId}")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','chef')")
    public Result<Void> startCooking(@PathVariable Long itemId) {
        OrderItem item = orderItemService.getById(itemId);
        if (item == null) return Result.error("菜品不存在");
        item.setKitchenStatus("COOKING");
        item.setCookingStartedAt(LocalDateTime.now());
        orderItemService.updateById(item);
        broadcast("kitchen", "item_updated", toVO(item));
        return Result.success();
    }

    /** 厨师：完成制作 */
    @PostMapping("/done/{itemId}")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','chef')")
    public Result<Void> doneCooking(@PathVariable Long itemId) {
        OrderItem item = orderItemService.getById(itemId);
        if (item == null) return Result.error("菜品不存在");
        item.setKitchenStatus("DONE");
        item.setDoneAt(LocalDateTime.now());
        orderItemService.updateById(item);
        KitchenItemVO vo = toVO(item);
        broadcast("kitchen", "item_updated", vo);
        broadcast("waiter", "item_done", vo);
        return Result.success();
    }

    // ========== 服务员端 ==========

    /** 服务员操控台：当前订单列表（含制作进度） */
    @GetMapping("/waiter")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','waiter','cashier')")
    public Result<List<Map<String, Object>>> waiterBoard(HttpServletRequest request) {
        Long storeId = getStoreIdFromToken(request);
        List<OrderItem> items = orderItemService.getInProgressItemsByStore(storeId);
        // 批量加载订单表号
        Map<Long, String> orderTableMap = buildOrderTableMap(items);
        Map<Long, List<OrderItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<Map<String, Object>> orders = new ArrayList<>();
        for (Map.Entry<Long, List<OrderItem>> e : grouped.entrySet()) {
            Map<String, Object> order = new LinkedHashMap<>();
            order.put("orderId", e.getKey());
            order.put("items", e.getValue().stream()
                    .map(i -> toVO(i, orderTableMap)).collect(Collectors.toList()));
            order.put("tableNumber", orderTableMap.getOrDefault(e.getKey(), ""));
            orders.add(order);
        }
        return Result.success(orders);
    }

    /** 服务员：催单 */
    @PostMapping("/bump/{itemId}")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','waiter','cashier')")
    public Result<Void> bump(@PathVariable Long itemId) {
        OrderItem item = orderItemService.getById(itemId);
        if (item == null) return Result.error("菜品不存在");
        item.setBumpCount((item.getBumpCount() == null ? 0 : item.getBumpCount()) + 1);
        item.setBumpedAt(LocalDateTime.now());
        orderItemService.updateById(item);
        KitchenItemVO vo = toVO(item);
        broadcast("kitchen", "item_bumped", vo);
        broadcast("waiter", "item_bumped", vo);
        return Result.success();
    }

    /** 服务员：上菜 */
    @PostMapping("/serve/{itemId}")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','waiter','cashier')")
    public Result<Void> serve(@PathVariable Long itemId) {
        OrderItem item = orderItemService.getById(itemId);
        if (item == null) return Result.error("菜品不存在");
        item.setKitchenStatus("SERVED");
        orderItemService.updateById(item);
        KitchenItemVO vo = toVO(item);
        broadcast("kitchen", "item_served", vo);
        broadcast("waiter", "item_served", vo);
        return Result.success();
    }

    /** 检查订单是否有未完成的厨房菜品（结账前警告用） */
    @GetMapping("/unfinished/{orderId}")
    public Result<Map<String, Object>> checkUnfinished(@PathVariable Long orderId) {
        List<OrderItem> items = orderItemService.getItemsByOrderId(orderId);
        List<KitchenItemVO> unfinished = items.stream()
                .filter(i -> "WAITING".equals(i.getKitchenStatus())
                          || "COOKING".equals(i.getKitchenStatus())
                          || "DONE".equals(i.getKitchenStatus()))
                .map(this::toVO)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasUnfinished", !unfinished.isEmpty());
        result.put("count", unfinished.size());
        result.put("items", unfinished);
        return Result.success(result);
    }

    // ========== 内部 ==========

    /** 批量查询订单表号，消除 N+1 查询 */
    private Map<Long, String> buildOrderTableMap(List<OrderItem> items) {
        Set<Long> orderIds = items.stream().map(OrderItem::getOrderId).collect(Collectors.toSet());
        if (orderIds.isEmpty()) return Map.of();
        Map<Long, String> map = new HashMap<>();
        for (Long oid : orderIds) {
            var order = orderService.getById(oid);
            if (order != null) map.put(oid, order.getTableNumber());
        }
        return map;
    }

    private KitchenItemVO toVO(OrderItem i, java.util.Map<Long, String> orderTableMap) {
        KitchenItemVO v = new KitchenItemVO();
        v.setId(i.getId());
        v.setOrderId(i.getOrderId());
        v.setDishName(i.getDishName());
        v.setKitchenStatus(i.getKitchenStatus() != null ? i.getKitchenStatus() : "WAITING");
        v.setBumpCount(i.getBumpCount() != null ? i.getBumpCount() : 0);
        v.setCreatedAt(i.getCreatedAt());
        v.setCookingStartedAt(i.getCookingStartedAt());
        v.setRemark(i.getRemark());
        v.setWaitSeconds(i.getCreatedAt() != null
                ? ChronoUnit.SECONDS.between(i.getCreatedAt(), LocalDateTime.now()) : 0);
        v.setTableNumber(orderTableMap.getOrDefault(i.getOrderId(), ""));
        return v;
    }

    private void broadcast(String topic, String event, Object data) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("event", event);
            msg.put("data", data);
            messaging.convertAndSend("/topic/" + topic, (Object) msg);
        } catch (Exception ignored) {}
    }

    private Long getStoreIdFromToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try { Claims c = jwtUtil.parseToken(auth.substring(7)); return c.get("storeId", Long.class); }
            catch (Exception ignored) {}
        }
        return null;
    }
}
