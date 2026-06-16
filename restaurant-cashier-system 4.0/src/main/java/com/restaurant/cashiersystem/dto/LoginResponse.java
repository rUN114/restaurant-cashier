package com.restaurant.cashiersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private Long storeId;        // 所属分店ID（可为null，表示总部账号）
    private String roleCode;     // 角色编码，用于前端权限控制
}