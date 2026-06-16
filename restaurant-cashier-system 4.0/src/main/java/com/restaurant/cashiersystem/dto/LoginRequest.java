package com.restaurant.cashiersystem.dto;

import lombok.Data;

import java.util.List;

/**
 * 登录请求参数
 */
@Data
public class LoginRequest {
    private String username;
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