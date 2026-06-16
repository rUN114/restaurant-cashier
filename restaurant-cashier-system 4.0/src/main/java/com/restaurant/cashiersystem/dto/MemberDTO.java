package com.restaurant.cashiersystem.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MemberDTO {
    private String phone;
    private String name;
    private Integer gender;
    private LocalDate birthday;
    private Long levelId;
    private Long storeId;
    private String remark;
}
