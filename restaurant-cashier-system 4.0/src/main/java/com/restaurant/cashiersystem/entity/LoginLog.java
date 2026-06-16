package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("login_logs")
public class LoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long storeId;
    private String loginIp;
    private String loginDevice;
    private Integer loginStatus;// 1-成功 0-失败
    private LocalDateTime createdAt;
}