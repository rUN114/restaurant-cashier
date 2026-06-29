package com.restaurant.cashiersystem.controller;

import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.dto.KitchenItemVO;
import com.restaurant.cashiersystem.entity.OrderItem;
import com.restaurant.cashiersystem.entity.Order;
import com.restaurant.cashiersystem.service.OrderItemService;
import com.restaurant.cashiersystem.service.OrderService;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KDS 后厨看板控制器")
class KDSControllerTest {

    @Mock private OrderItemService orderItemService;
    @Mock private OrderService orderService;
    @Mock private JwtUtil jwtUtil;
    @Mock private SimpMessagingTemplate messaging;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private KDSController controller;

    private OrderItem waitingItem;
    private OrderItem cookingItem;
    private OrderItem doneItem;

    @BeforeEach
    void setUp() {
        waitingItem = new OrderItem();
        waitingItem.setId(1L);
        waitingItem.setOrderId(100L);
        waitingItem.setDishName("红烧肉");
        waitingItem.setKitchenStatus("WAITING");
        waitingItem.setCreatedAt(LocalDateTime.now());

        cookingItem = new OrderItem();
        cookingItem.setId(2L);
        cookingItem.setOrderId(101L);
        cookingItem.setDishName("宫保鸡丁");
        cookingItem.setKitchenStatus("COOKING");
        cookingItem.setCreatedAt(LocalDateTime.now().minusMinutes(15));

        doneItem = new OrderItem();
        doneItem.setId(3L);
        doneItem.setOrderId(102L);
        doneItem.setDishName("酸辣汤");
        doneItem.setKitchenStatus("DONE");
        doneItem.setCreatedAt(LocalDateTime.now().minusMinutes(20));
    }

    @Test
    @DisplayName("厨房看板：WAITING/COOKING/DONE 分组返回")
    void kitchenBoardGroupsByStatus() {
        when(request.getHeader("Authorization")).thenReturn("Bearer test");
        when(jwtUtil.parseToken("test")).thenReturn(mock(Claims.class));
        when(orderItemService.getInProgressItemsByStore(any())).thenReturn(
                List.of(waitingItem, cookingItem, doneItem));
        when(orderService.getById(anyLong())).thenReturn(new Order());

        Result<Map<String, List<KitchenItemVO>>> result = controller.kitchenBoard(request);

        assertNotNull(result);
        Map<String, List<KitchenItemVO>> data = result.getData();
        assertEquals(1, data.get("WAITING").size());
        assertEquals(1, data.get("COOKING").size());
        assertEquals(1, data.get("DONE").size());
        assertEquals("红烧肉", data.get("WAITING").get(0).getDishName());
    }

    @Test
    @DisplayName("厨师开始制作：状态 WAITING→COOKING")
    void startCookingChangesStatus() {
        when(orderItemService.getById(1L)).thenReturn(waitingItem);
        when(orderItemService.updateById(any())).thenReturn(true);

        Result<Void> result = controller.startCooking(1L);

        assertNotNull(result);
        assertEquals("COOKING", waitingItem.getKitchenStatus());
        assertNotNull(waitingItem.getCookingStartedAt());
        verify(messaging, atLeastOnce()).convertAndSend(eq("/topic/kitchen"), any(Object.class));
    }

    @Test
    @DisplayName("厨师完成制作：状态 COOKING→DONE，广播到厨房+服务员")
    void doneCookingBroadcastsToBoth() {
        when(orderItemService.getById(2L)).thenReturn(cookingItem);
        when(orderItemService.updateById(any())).thenReturn(true);

        Result<Void> result = controller.doneCooking(2L);

        assertNotNull(result);
        assertEquals("DONE", cookingItem.getKitchenStatus());
        verify(messaging, atLeastOnce()).convertAndSend(eq("/topic/kitchen"), any(Object.class));
        verify(messaging).convertAndSend(eq("/topic/waiter"), any(Object.class));
    }

    @Test
    @DisplayName("催单：bumpCount 自增，广播到双方")
    void bumpIncrementsCount() {
        when(orderItemService.getById(1L)).thenReturn(waitingItem);
        when(orderItemService.updateById(any())).thenReturn(true);

        controller.bump(1L);

        assertEquals(1, waitingItem.getBumpCount());
        verify(messaging, atLeastOnce()).convertAndSend(eq("/topic/kitchen"), any(Object.class));
        verify(messaging).convertAndSend(eq("/topic/waiter"), any(Object.class));
    }

    @Test
    @DisplayName("上菜：状态 → SERVED")
    void serveMarksAsServed() {
        when(orderItemService.getById(3L)).thenReturn(doneItem);
        when(orderItemService.updateById(any())).thenReturn(true);

        Result<Void> result = controller.serve(3L);

        assertNotNull(result);
        assertEquals("SERVED", doneItem.getKitchenStatus());
        verify(messaging, atLeastOnce()).convertAndSend(eq("/topic/kitchen"), any(Object.class));
    }

    @Test
    @DisplayName("菜品不存在返回错误")
    void returnsErrorWhenItemNotFound() {
        when(orderItemService.getById(999L)).thenReturn(null);

        Result<Void> result = controller.startCooking(999L);
        assertNotNull(result);
        assertTrue(result.getMsg().contains("不存在"));
    }

    @Test
    @DisplayName("createdAt 为 null 时不抛 NPE")
    void handlesNullCreatedAt() {
        waitingItem.setCreatedAt(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer test");
        when(jwtUtil.parseToken("test")).thenReturn(mock(Claims.class));
        when(orderItemService.getInProgressItemsByStore(any())).thenReturn(List.of(waitingItem));
        when(orderService.getById(anyLong())).thenReturn(new Order());

        Result<Map<String, List<KitchenItemVO>>> result = controller.kitchenBoard(request);

        assertNotNull(result);
        assertEquals(0, result.getData().get("WAITING").get(0).getWaitSeconds());
    }

    @Test
    @DisplayName("未完成菜品检查：返回 WAITING/COOKING/DONE 的菜品")
    void checkUnfinishedReturnsOnlyActiveItems() {
        when(orderItemService.getItemsByOrderId(100L)).thenReturn(
                List.of(waitingItem, cookingItem, doneItem));

        Result<Map<String, Object>> result = controller.checkUnfinished(100L);

        assertNotNull(result);
        Map<String, Object> data = result.getData();
        assertEquals(true, data.get("hasUnfinished"));
        assertEquals(3, ((List<?>) data.get("items")).size());
    }
}
