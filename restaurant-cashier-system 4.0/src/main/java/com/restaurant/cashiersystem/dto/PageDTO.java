package com.restaurant.cashiersystem.dto;

import lombok.Data;

/**
 * 分页查询DTO
 */
@Data
public class PageDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}