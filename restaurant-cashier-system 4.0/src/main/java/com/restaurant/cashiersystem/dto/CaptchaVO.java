package com.restaurant.cashiersystem.dto;

import lombok.Data;

/**
 * 验证码响应（返回给前端用于渲染滑块）
 */
@Data
public class CaptchaVO {
    private String captchaId;
    private String backgroundImage;  // base64 背景图
    private String sliderImage;      // base64 滑块图
    private Integer bgWidth;
    private Integer bgHeight;
    private Integer sliderWidth;
    private Integer sliderHeight;
}
