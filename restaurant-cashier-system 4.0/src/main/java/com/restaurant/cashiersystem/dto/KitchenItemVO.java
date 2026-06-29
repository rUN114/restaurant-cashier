package com.restaurant.cashiersystem.dto;

import lombok.Data;
import java.time.LocalDateTime;

/** 厨师看板单条菜品卡片 */
@Data
public class KitchenItemVO {
    private Long id;             // orderItemId
    private Long orderId;
    private String tableNumber;
    private String dishName;
    private String spec;         // 规格
    private Integer quantity;
    private String kitchenStatus; // WAITING/COOKING/DONE/SERVED
    private Integer bumpCount;
    private boolean isNewDish;    // 加菜标记
    private String remark;         // 备注（少辣、不要香菜等）
    private LocalDateTime createdAt;
    private LocalDateTime cookingStartedAt;
    private long waitSeconds;     // 已等待秒数
}
