package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.Member;
import com.restaurant.cashiersystem.entity.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.restaurant.cashiersystem.entity.OperationLog;
import com.restaurant.cashiersystem.entity.Order;
import com.restaurant.cashiersystem.entity.OrderItem;
import com.restaurant.cashiersystem.mapper.OrderMapper;
import com.restaurant.cashiersystem.service.MaterialConsumptionService;
import com.restaurant.cashiersystem.service.OperationLogService;
import com.restaurant.cashiersystem.service.OrderItemService;
import com.restaurant.cashiersystem.service.OrderService;
import com.restaurant.cashiersystem.service.MemberService;
import com.restaurant.cashiersystem.service.SysConfigService;
import com.restaurant.cashiersystem.service.TableService;
import com.restaurant.cashiersystem.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

        // ========== 记录操作日志 ==========
        try {
            OperationLog log = new OperationLog();
            log.setStoreId(order.getStoreId());
            log.setOperatorId(cashierId);
            log.setTargetType("order");
            log.setTargetId(orderId.toString());
            log.setAction("settle");
            log.setDetail(String.format("结账成功，支付方式：%s，折扣：%s，实收：%s，会员：%s",
                    paymentMethod, effectiveDiscount, actualFinal,
                    memberId != null ? "是" : "否"));
            log.setCreatedAt(LocalDateTime.now());
            operationLogService.save(log);
        } catch (Exception e) {
            // 日志记录失败不影响主业务
        }
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

        // 3. 记录操作日志（异常不影响退单主流程）
        try {
            OperationLog logEntry = new OperationLog();
            logEntry.setStoreId(order.getStoreId());
            logEntry.setOperatorId(operatorId);
            logEntry.setTargetType("order");
            logEntry.setTargetId(orderId.toString());
            logEntry.setAction("cancel");
            logEntry.setDetail("退单，桌号：" +
                    (order.getTableNumber() != null ? order.getTableNumber() : "无"));
            logEntry.setCreatedAt(LocalDateTime.now());
            operationLogService.save(logEntry);
        } catch (Exception e) {
            log.warn("退单操作日志记录失败", e);
        }
    }

    @Override
    public BigDecimal getStoreTurnover(Long storeId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStoreId, storeId);
        wrapper.eq(Order::getStatus, "settled");
        List<Order> orders = list(wrapper);
        BigDecimal total = BigDecimal.ZERO;
        for (Order o : orders) {
            if (o.getFinalAmount() != null) {
                total = total.add(o.getFinalAmount());
            }
        }
        return total;
    }

    @Override
    public BigDecimal getTodayTurnover(Long storeId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStoreId, storeId);
        wrapper.eq(Order::getStatus, "settled");
        wrapper.ge(Order::getCreatedAt, LocalDateTime.now().toLocalDate().atStartOfDay());
        List<Order> orders = list(wrapper);
        BigDecimal total = BigDecimal.ZERO;
        for (Order o : orders) {
            if (o.getFinalAmount() != null) {
                total = total.add(o.getFinalAmount());
            }
        }
        return total;
    }

    @Override
    public BigDecimal getMonthTurnover(Long storeId) {
        LocalDateTime firstDayOfMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStoreId, storeId);
        wrapper.eq(Order::getStatus, "settled");
        wrapper.ge(Order::getCreatedAt, firstDayOfMonth);
        List<Order> orders = list(wrapper);
        BigDecimal total = BigDecimal.ZERO;
        for (Order o : orders) {
            if (o.getFinalAmount() != null) {
                total = total.add(o.getFinalAmount());
            }
        }
        return total;
    }
}