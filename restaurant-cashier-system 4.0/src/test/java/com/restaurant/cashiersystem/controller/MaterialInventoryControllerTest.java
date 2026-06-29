package com.restaurant.cashiersystem.controller;

import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.Material;
import com.restaurant.cashiersystem.entity.MaterialInventory;
import com.restaurant.cashiersystem.entity.Purchase;
import com.restaurant.cashiersystem.service.MaterialInventoryService;
import com.restaurant.cashiersystem.service.MaterialService;
import com.restaurant.cashiersystem.service.PurchaseService;
import com.restaurant.cashiersystem.service.StockRecordService;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("库存管理控制器 — 含单位换算")
class MaterialInventoryControllerTest {

    @Mock private MaterialInventoryService materialInventoryService;
    @Mock private MaterialService materialService;
    @Mock private PurchaseService purchaseService;
    @Mock private StockRecordService stockRecordService;
    @Mock private JwtUtil jwtUtil;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private MaterialInventoryController controller;

    @BeforeEach
    void setUp() {
        when(request.getHeader("Authorization")).thenReturn("Bearer test");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(jwtUtil.parseToken("test")).thenReturn(claims);
    }

    @Test
    @DisplayName("进货：无单位换算时 数量=库存数量")
    void adjustInventoryNoConversion() {
        Material beef = new Material();
        beef.setId(1L);
        beef.setUnit("g");
        beef.setPurchaseUnit("斤");
        beef.setUnitRate(new BigDecimal("500"));

        MaterialInventory inv = new MaterialInventory();
        inv.setCurrentStock(new BigDecimal("1000"));
        inv.setStoreId(1L);
        inv.setMaterialId(1L);

        when(materialService.getById(1L)).thenReturn(beef);
        when(materialInventoryService.getOne(any())).thenReturn(inv);
        when(materialInventoryService.updateById(any())).thenReturn(true);
        when(stockRecordService.save(any())).thenReturn(true);
        when(purchaseService.save(any())).thenReturn(true);

        Map<String, Object> body = Map.of(
                "storeId", 1, "materialId", 1, "quantity", 4, "unitPrice", 42, "supplier", "肉批"
        );

        Result<Void> result = controller.adjustInventory(body, request);

        assertNotNull(result);

        // 库存应按换算率增加：4斤 × 500 = 2000g
        ArgumentCaptor<MaterialInventory> invCaptor = ArgumentCaptor.forClass(MaterialInventory.class);
        verify(materialInventoryService).updateById(invCaptor.capture());
        assertEquals(0, new BigDecimal("3000").compareTo(invCaptor.getValue().getCurrentStock())); // 1000+2000

        // 采购记录应存采购单位数量
        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseService).save(purchaseCaptor.capture());
        Purchase p = purchaseCaptor.getValue();
        assertEquals(0, new BigDecimal("4").compareTo(p.getQuantity()));
        assertEquals(0, new BigDecimal("42").compareTo(p.getUnitPrice()));
        assertEquals(0, new BigDecimal("168").compareTo(p.getTotalPrice())); // 4×42
    }

    @Test
    @DisplayName("进货：新建库存记录时自动填成本价")
    void newInventoryHasCostPrice() {
        Material material = new Material();
        material.setId(2L);
        material.setUnit("g");
        material.setUnitRate(BigDecimal.ONE);

        when(materialService.getById(2L)).thenReturn(material);
        when(materialInventoryService.getOne(any())).thenReturn(null);  // 无存量
        when(materialInventoryService.save(any())).thenReturn(true);
        when(stockRecordService.save(any())).thenReturn(true);
        when(purchaseService.save(any())).thenReturn(true);

        Map<String, Object> body = Map.of(
                "storeId", 1, "materialId", 2, "quantity", 10, "unitPrice", 35
        );

        controller.adjustInventory(body, request);

        ArgumentCaptor<MaterialInventory> captor = ArgumentCaptor.forClass(MaterialInventory.class);
        verify(materialInventoryService).save(captor.capture());
        assertEquals(0, new BigDecimal("35").compareTo(captor.getValue().getCostPrice()));
        assertEquals(0, new BigDecimal("10").compareTo(captor.getValue().getCurrentStock()));
    }

    @Test
    @DisplayName("出库：库存不足时返回错误")
    void stockOutInsufficientReturnsError() {
        when(materialService.getById(anyLong())).thenReturn(null);
        when(materialInventoryService.getOne(any())).thenReturn(null);

        Map<String, Object> body = Map.of(
                "storeId", 1, "materialId", 1, "quantity", -100
        );

        Result<Void> result = controller.adjustInventory(body, request);

        assertNotNull(result);
        assertTrue(result.getMsg().contains("无库存"));
    }

    @Test
    @DisplayName("回填成本价：从采购记录取值")
    void backfillCostPriceFromPurchases() {
        MaterialInventory inv1 = new MaterialInventory();
        inv1.setId(1L); inv1.setStoreId(1L); inv1.setMaterialId(1L);
        inv1.setCostPrice(null); inv1.setCurrentStock(BigDecimal.TEN);

        MaterialInventory inv2 = new MaterialInventory();
        inv2.setId(2L); inv2.setStoreId(1L); inv2.setMaterialId(2L);
        inv2.setCostPrice(new BigDecimal("20")); inv2.setCurrentStock(BigDecimal.TEN);

        Purchase purchase = new Purchase();
        purchase.setUnitPrice(new BigDecimal("15"));

        when(materialInventoryService.list()).thenReturn(java.util.List.of(inv1, inv2));
        when(purchaseService.getOne(any())).thenReturn(purchase);
        when(materialInventoryService.updateById(any())).thenReturn(true);

        Result<Map<String, Object>> result = controller.backfillCostPrice();

        assertNotNull(result);
        Map<String, Object> data = result.getData();
        assertEquals(2, ((Number) data.get("total")).intValue());
        assertEquals(1, ((Number) data.get("updated")).intValue());
        assertEquals(1, ((Number) data.get("skipped")).intValue());
    }
}
