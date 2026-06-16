package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("roles")
public class    Role {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roleCode;    // 角色编码(程序鉴权用)

    private String name;        // 角色名称

    private String description; // 描述

    private LocalDateTime createdAt;
}