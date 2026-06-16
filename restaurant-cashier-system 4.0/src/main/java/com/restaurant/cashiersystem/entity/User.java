package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("users")
public class User {
    @TableLogic

    @TableId(type = IdType.AUTO)
    private Long id;                // 员工ID

    private Long storeId;           // 所属分店ID，总部为null

    private Long roleId;            // 角色ID

    private String username;        // 登录账号(全局唯一)

    private String password;        // bcrypt加密密码

    private String realName;        // 真实姓名

    private String phone;           // 手机号

    private Integer status;         // 1-在职 0-禁用(离职)

    // 🔥 插入时自动填充
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // 🔥 插入和更新时都自动填充
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 软删除字段
     */
    @TableLogic  // 🔥 这里加上 MyBatis-Plus 软删除注解
    @TableField(value = "deleted_at")
    private LocalDateTime deletedAt;

    // 关联对象（非数据库字段）
    @TableField(exist = false)
    private Store store;

    @TableField(exist = false)
    private Role role;
}