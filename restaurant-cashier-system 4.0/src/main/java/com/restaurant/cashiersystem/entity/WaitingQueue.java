package com.restaurant.cashiersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("waiting_queue")
public class WaitingQueue {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private String customerName;
    private String phone;
    private Integer partySize;
    private String tablePref;
    private String queueNumber;
    private String status;      // waiting / seated / cancelled
    private String seatedTable;
    private LocalDateTime createdAt;
    private LocalDateTime seatedAt;
}
