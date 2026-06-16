package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("changelogs")
public class Changelog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String version;
    private String title;
    private String content;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
