package com.restaurant.cashiersystem.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户信息传输对象（用于新增/修改）
 */
@Getter
@Setter
public class UserDTO {
    private Long storeId;        // 所属分店ID（总部为null）
    private Long roleId;         // 角色ID
    private String username;     // 登录账号（全局唯一）
    private String password;     // 明文密码（接收前端传入，后端加密后再存）
    private String realName;     // 真实姓名
    private String phone;        // 手机号
    private Integer status;      // 1-在职 0-禁用，默认可不传则由后台设默认值
}