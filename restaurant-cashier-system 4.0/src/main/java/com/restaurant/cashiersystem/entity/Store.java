package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stores")
public class Store {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;        // 分店名称

    private String address;     // 地址

    private String phone;       // 联系电话

    private Integer status;     // 1-营业 0-停用

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(value = "deleted_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime deletedAt;  // 软删除，查询时注意过滤 NULL
}