package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.MaterialConsumption;
import com.restaurant.cashiersystem.entity.MaterialInventory;
import com.restaurant.cashiersystem.entity.Member;
import com.restaurant.cashiersystem.entity.MemberPointLog;
import com.restaurant.cashiersystem.entity.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.restaurant.cashiersystem.entity.OperationLog;
import com.restaurant.cashiersystem.entity.Order;
import com.restaurant.cashiersystem.entity.OrderItem;
import com.restaurant.cashiersystem.mapper.OrderMapper;
import com.restaurant.cashiersystem.service.AsyncLogService;
import com.restaurant.cashiersystem.service.MaterialConsumptionService;
import com.restaurant.cashiersystem.service.MaterialInventoryService;
import com.restaurant.cashiersystem.service.MemberPointLogService;
import com.restaurant.cashiersystem.service.OperationLogService;
import com.restaurant.cashiersystem.service.OrderItemService;
import com.restaurant.cashiersystem.service.OrderService;
import com.restaurant.cashiersystem.service.MemberService;
import com.restaurant.cashiersystem.service.SysConfigService;
import com.restaurant.cashiersystem.service.TableService;
import com.restaurant.cashiersystem.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Lazy
    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private MaterialConsumptionService materialConsumptionService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private TableService tableService;

    @Autowired
    private MaterialInventoryService materialInventoryService;

    @Autowired
    private MemberPointLogService memberPointLogService;

    @Autowired
    private AsyncLogService asyncLogService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<Order> getOrdersByStore(Long storeId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStoreId, storeId);
        return list(wrapper);
    }

    @Override
    public List<Order> getInProgressOrders(Long storeId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStoreId, storeId);
        wrapper.eq(Order::getStatus, "in_progress");
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleOrder(Long orderId, Long cashierId, String paymentMethod,
                            BigDecimal discountAmount, BigDecimal finalAmount,
                            Long memberId, boolean useMemberDiscount, int redeemPoints) {
        Order order = getById(orderId);
        if (order == null) {
            return;
        }
        // 防止重复结算：Controller 层幂等检查之外的第二道防线
        if (!"in_progress".equals(order.getStatus())) {
            throw new RuntimeException("订单状态为 " + order.getStatus() + "，无法结算");
        }

        // 从订单明细计算原始总金额
        List<OrderItem> items = orderItemService.getItemsByOrderId(orderId);
        BigDecimal total = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ---- 会员积分处理（异常不影响结账主流程） ----
        BigDecimal settledAmount = finalAmount != null ? finalAmount : total;
        if (memberId != null && memberId > 0) {
            try {
                Member member = memberService.getById(memberId);
                if (member != null && member.getStatus() == 1) {
                    if (redeemPoints > 0 && !useMemberDiscount) {
                        memberService.deductPoints(memberId, redeemPoints);
                    }
                    int pointsPerYuan = sysConfigService.getIntValue("member.points_per_yuan", 1);
                    int earnedPoints = settledAmount.intValue() * pointsPerYuan;
                    memberService.addConsumptionAndPoints(memberId, settledAmount,
                            Math.max(0, earnedPoints));
                }
            } catch (Exception e) {
                log.error("会员积分处理失败，结账继续: memberId={}", memberId, e);
            }
        }

        order.setTotalAmount(total);
        // 计算实际折扣（total - finalAmount），包含手动抹零和会员折扣
        BigDecimal actualFinal = finalAmount != null ? finalAmount : total;
        BigDecimal effectiveDiscount = total.subtract(actualFinal);
        order.setDiscountAmount(effectiveDiscount);
        order.setFinalAmount(actualFinal);
        order.setStatus("settled");
        order.setPaymentMethod(paymentMethod);
        order.setSettledAt(LocalDateTime.now());
        order.setSettledBy(cashierId);

        updateById(order);

        // 扣减原材料库存（同一事务，库存不足则订单结账也回滚）
        materialConsumptionService.consumeOrderMaterials(orderId);

        // 异步记录操作日志（不阻塞结账响应）
        OperationLog settleLog = new OperationLog();
        settleLog.setStoreId(order.getStoreId());
        settleLog.setOperatorId(cashierId);
        settleLog.setTargetType("order");
        settleLog.setTargetId(orderId.toString());
        settleLog.setAction("settle");
        settleLog.setDetail(String.format("结账成功，支付方式：%s，折扣：%s，实收：%s，会员：%s",
                paymentMethod, effectiveDiscount, actualFinal,
                memberId != null ? "是" : "否"));
        settleLog.setCreatedAt(LocalDateTime.now());
        asyncLogService.saveOperationLog(settleLog);
        log.info("订单结账成功: orderId={}, orderNo={}, 实收={}, 支付={}, 会员={}",
                orderId, order.getOrderNo(), actualFinal, paymentMethod,
                memberId != null ? memberId : "无");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long operatorId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!"in_progress".equals(order.getStatus())) {
            throw new RuntimeException("只有进行中的订单才能取消");
        }

        // 1. 更新订单状态为已取消
        order.setStatus("cancelled");
        updateById(order);

        // 2. 释放桌台
        if (order.getTableId() != null) {
            Table table = tableService.getById(order.getTableId());
            if (table != null && "occupied".equals(table.getStatus())) {
                table.setStatus("free");
                tableService.updateById(table);
            }
        }

        // 3. 异步记录操作日志
        OperationLog cancelLog = new OperationLog();
        cancelLog.setStoreId(order.getStoreId());
        cancelLog.setOperatorId(operatorId);
        cancelLog.setTargetType("order");
        cancelLog.setTargetId(orderId.toString());
        cancelLog.setAction("cancel");
        cancelLog.setDetail("退单，桌号：" +
                (order.getTableNumber() != null ? order.getTableNumber() : "无"));
        cancelLog.setCreatedAt(LocalDateTime.now());
        asyncLogService.saveOperationLog(cancelLog);
        log.info("订单退单: orderId={}, orderNo={}, 桌号={}, 操作人={}",
                orderId, order.getOrderNo(), order.getTableNumber(), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseSettlement(Long orderId, Long operatorId) {
        Order order = getById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (!"settled".equals(order.getStatus())) throw new RuntimeException("仅已结算订单可反结账");

        // 1. 回滚库存：查询该订单的原料消耗记录，加回库存
        LambdaQueryWrapper<MaterialConsumption> cWrapper = new LambdaQueryWrapper<>();
        cWrapper.eq(MaterialConsumption::getOrderId, orderId);
        List<MaterialConsumption> consumptions = materialConsumptionService.list(cWrapper);

        for (MaterialConsumption mc : consumptions) {
            MaterialInventory inv = materialInventoryService.lambdaQuery()
                    .eq(MaterialInventory::getStoreId, mc.getStoreId())
                    .eq(MaterialInventory::getMaterialId, mc.getMaterialId())
                    .last("FOR UPDATE")
                    .one();
            if (inv != null) {
                inv.setCurrentStock(inv.getCurrentStock().add(mc.getUsedQuantity()));
                materialInventoryService.updateById(inv);
            }
        }

        // 2. 物理删除消耗记录
        if (!consumptions.isEmpty()) {
            materialConsumptionService.remove(cWrapper);
        }

        // 3. 回滚会员积分：从积分日志反查该订单涉及的会员和变动
        BigDecimal refundAmount = order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO;
        try {
            LambdaQueryWrapper<MemberPointLog> logWrapper = new LambdaQueryWrapper<>();
            logWrapper.eq(MemberPointLog::getOrderId, orderId);
            List<MemberPointLog> logs = memberPointLogService.list(logWrapper);
            if (!logs.isEmpty()) {
                Long memberId = logs.get(0).getMemberId();
                int totalPointsChange = logs.stream().mapToInt(MemberPointLog::getChangePoints).sum();
                // 正数=当时加了积分(现在扣回)，负数=当时扣了积分(现在加回)
                if (totalPointsChange != 0) {
                    if (totalPointsChange > 0) {
                        memberService.deductPoints(memberId, totalPointsChange);
                    } else {
                        memberService.addPoints(memberId, -totalPointsChange);
                    }
                }
                // 回退累计消费
                memberService.refundRollback(memberId, refundAmount,
                        -totalPointsChange);
            }
        } catch (Exception e) {
            log.warn("反结账-会员积分回滚失败: orderId={}", orderId, e);
        }

        // 4. 订单状态回退
        order.setStatus("in_progress");
        order.setPaymentMethod(null);
        order.setSettledAt(null);
        order.setSettledBy(null);
        updateById(order);

        // 5. 异步记录操作日志
        OperationLog reverseLog = new OperationLog();
        reverseLog.setStoreId(order.getStoreId());
        reverseLog.setOperatorId(operatorId);
        reverseLog.setTargetType("order");
        reverseLog.setTargetId(orderId.toString());
        reverseLog.setAction("reverseSettle");
        reverseLog.setDetail("反结账，订单号：" + (order.getOrderNo() != null ? order.getOrderNo() : orderId)
                + "，原实收：" + (refundAmount));
        reverseLog.setCreatedAt(LocalDateTime.now());
        asyncLogService.saveOperationLog(reverseLog);
        log.info("反结账完成: orderId={}, orderNo={}, 原实收={}, 操作人={}",
                orderId, order.getOrderNo(), refundAmount, operatorId);
    }

    @Override
    public BigDecimal getStoreTurnover(Long storeId) {
        // SQL 聚合替代 Java 循环 SUM，避免全量订单拉入内存
        String sql = "SELECT COALESCE(SUM(final_amount),0) FROM orders WHERE status='settled' AND store_id=?";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, storeId);
    }

    @Override
    public BigDecimal getTodayTurnover(Long storeId) {
        String sql = "SELECT COALESCE(SUM(final_amount),0) FROM orders WHERE status='settled' AND store_id=? AND DATE(settled_at)=CURDATE()";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, storeId);
    }

    @Override
    public BigDecimal getMonthTurnover(Long storeId) {
        String sql = "SELECT COALESCE(SUM(final_amount),0) FROM orders WHERE status='settled' AND store_id=? AND YEAR(settled_at)=YEAR(NOW()) AND MONTH(settled_at)=MONTH(NOW())";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, storeId);
    }
}