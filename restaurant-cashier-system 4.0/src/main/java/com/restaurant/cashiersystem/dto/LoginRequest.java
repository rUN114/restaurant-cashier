package com.restaurant.cashiersystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 登录请求参数
 */
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度需在2~50之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 1, max = 128, message = "密码长度需在1~128之间")
    private String password;

    // 验证码相关字段
    private String captchaId;
    private Integer bgImageWidth;
    private Integer bgImageHeight;
    private Integer sliderImageWidth;
    private Integer sliderImageHeight;
    private Long startTime;
    private Long stopTime;
    private List<TrackPoint> trackList;

    @Data
    public static class TrackPoint {
        private Integer x;
        private Integer y;
        private Long t;
    }
}