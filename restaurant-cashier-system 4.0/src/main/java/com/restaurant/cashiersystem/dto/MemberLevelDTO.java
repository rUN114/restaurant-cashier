package com.restaurant.cashiersystem.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MemberLevelDTO {
    private String name;
    private Integer sortOrder;
    private BigDecimal minConsumption;
    private BigDecimal discountRate;
    private String icon;
    private String color;
    private String remark;
    private Integer status;
}
