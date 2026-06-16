package com.restaurant.cashiersystem.dto;

import lombok.Data;

/**
 * 修改密码DTO
 */
@Data
public class PasswordUpdateDTO {
    private String oldPassword;
    private String newPassword;
}