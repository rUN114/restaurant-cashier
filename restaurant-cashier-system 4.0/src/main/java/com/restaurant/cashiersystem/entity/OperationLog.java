package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_logs")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private Long operatorId;
    private String targetType;
    private String targetId;
    private String action;
    private String detail;
    private LocalDateTime createdAt;
}