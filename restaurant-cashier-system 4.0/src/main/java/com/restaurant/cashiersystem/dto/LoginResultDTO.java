package com.restaurant.cashiersystem.dto;

import lombok.Data;

@Data
public class LoginResultDTO {
    private Long userId;
    private String username;
    private String realName;
    private Long storeId;
    private String storeName;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private String token;          // JWT token
}
